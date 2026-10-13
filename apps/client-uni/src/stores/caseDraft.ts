import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { CaseView, FileView, InvoiceRequest, MaterialView, SimpleRequest, TemplateView } from '../api/generated/models'
import { createCaseDraft, getCaseDetail, getMyCasePage, saveInvoice, saveMaterialText, saveSimpleInfo, submitCase } from '../api/case'
import { deleteMaterialFile, getMaterialFiles, uploadCaseMaterial } from '../api/file'
import { getProductTemplate } from '../api/product'
import { errorMessage } from '../api/response'

const CURRENT_DRAFT_STORAGE_KEY = 'wqst_client_current_case_id'

function currentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function operationKey(storageKey: string) {
  const saved = String(uni.getStorageSync(storageKey) || '')
  if (saved) return saved
  const value = `${storageKey}-${Date.now()}-${Math.random().toString(16).slice(2)}`
  uni.setStorageSync(storageKey, value)
  return value
}

function matchesTradeDraft(detail: CaseView, companyId: number, productId: number, month: string) {
  return detail.companyId === companyId
    && detail.productId === productId
    && detail.serviceType === 'TRADE_INCREMENT'
    && detail.caseMonth === month
    && ['DRAFT', 'NEED_SUPPLEMENT'].includes(detail.status || '')
}

function matchesSimpleDraft(detail: CaseView, companyId: number, productId: number, serviceType: string) {
  return detail.companyId === companyId
    && detail.productId === productId
    && detail.serviceType === serviceType
    && ['DRAFT', 'NEED_SUPPLEMENT'].includes(detail.status || '')
}

export const useCaseDraftStore = defineStore('client-case-draft', () => {
  const caseDetail = ref<CaseView>()
  const template = ref<TemplateView>()
  const filesByMaterial = ref<Record<number, FileView[]>>({})
  const loading = ref(false)
  const submitting = ref(false)
  const busyMaterialId = ref<number>()
  const error = ref('')

  const materials = computed(() => [...(caseDetail.value?.materials || [])].sort((a, b) => {
    const aOrder = template.value?.items?.find((item) => item.itemCode === a.materialCode)?.sortOrder || 0
    const bOrder = template.value?.items?.find((item) => item.itemCode === b.materialCode)?.sortOrder || 0
    return aOrder - bOrder
  }))
  const isEditable = computed(() => ['DRAFT', 'NEED_SUPPLEMENT'].includes(caseDetail.value?.status || ''))
  const completedCount = computed(() => caseDetail.value?.progress?.completed || 0)
  const totalCount = computed(() => caseDetail.value?.progress?.total || materials.value.length)
  const completionPercent = computed(() => totalCount.value ? Math.round(completedCount.value / totalCount.value * 100) : 0)

  async function loadFiles(materialsToLoad: MaterialView[]) {
    const fileMaterials = materialsToLoad.filter((item) => item.inputType === 'FILE' && item.id)
    const entries = await Promise.all(fileMaterials.map(async (material) => [
      material.id as number,
      await getMaterialFiles(material.id as number),
    ] as const))
    filesByMaterial.value = Object.fromEntries(entries)
  }

  async function loadCase(caseId: number, showLoading = true) {
    if (showLoading) loading.value = true
    error.value = ''
    try {
      const detail = await getCaseDetail(caseId)
      if (!detail.productId) throw new Error('报单缺少产品编号')
      const [productTemplate] = await Promise.all([
        getProductTemplate(detail.productId),
        loadFiles(detail.materials || []),
      ])
      caseDetail.value = detail
      template.value = productTemplate
      if (detail.serviceType === 'TRADE_INCREMENT' && ['DRAFT', 'NEED_SUPPLEMENT'].includes(detail.status || '')) {
        uni.setStorageSync(CURRENT_DRAFT_STORAGE_KEY, caseId)
      } else if (Number(uni.getStorageSync(CURRENT_DRAFT_STORAGE_KEY)) === caseId) {
        uni.removeStorageSync(CURRENT_DRAFT_STORAGE_KEY)
      }
      return detail
    } catch (cause) {
      error.value = errorMessage(cause, '读取报单草稿失败')
      throw cause
    } finally {
      if (showLoading) loading.value = false
    }
  }

  async function prepareTradeDraft(companyId: number, productId: number) {
    loading.value = true
    error.value = ''
    try {
      const month = currentMonth()
      const storedCaseId = Number(uni.getStorageSync(CURRENT_DRAFT_STORAGE_KEY))
      if (Number.isFinite(storedCaseId) && storedCaseId > 0) {
        try {
          const storedDetail = await getCaseDetail(storedCaseId)
          if (matchesTradeDraft(storedDetail, companyId, productId, month)) {
            return await loadCase(storedCaseId, false)
          }
        } catch {
          // 本地草稿编号可能已失效，继续以服务端列表为准。
        }
        uni.removeStorageSync(CURRENT_DRAFT_STORAGE_KEY)
      }

      const page = await getMyCasePage(1, 50)
      const candidates = (page.items || []).filter((item) =>
        item.serviceType === 'TRADE_INCREMENT'
        && item.caseMonth === month
        && ['DRAFT', 'NEED_SUPPLEMENT'].includes(item.status || '')
        && item.id,
      )
      for (const candidate of candidates) {
        const detail = await getCaseDetail(candidate.id as number)
        if (matchesTradeDraft(detail, companyId, productId, month)) {
          return await loadCase(candidate.id as number, false)
        }
      }

      const keyStorage = `wqst_create_case_key_${companyId}_${productId}_${month}`
      const detail = await createCaseDraft({
        companyId,
        productId,
        serviceType: 'TRADE_INCREMENT',
        caseMonth: month,
      }, operationKey(keyStorage))
      uni.removeStorageSync(keyStorage)
      if (!detail.id) throw new Error('创建草稿后未返回报单编号')
      return await loadCase(detail.id, false)
    } catch (cause) {
      error.value = errorMessage(cause, '准备贸易增量草稿失败')
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function prepareSimpleDraft(companyId: number, productId: number, serviceType: 'IP' | 'QUALIFICATION') {
    loading.value = true
    error.value = ''
    try {
      const page = await getMyCasePage(1, 50)
      const candidates = (page.items || []).filter((item) =>
        item.serviceType === serviceType
        && ['DRAFT', 'NEED_SUPPLEMENT'].includes(item.status || '')
        && item.id,
      )
      for (const candidate of candidates) {
        const detail = await getCaseDetail(candidate.id as number)
        if (matchesSimpleDraft(detail, companyId, productId, serviceType)) {
          return await loadCase(candidate.id as number, false)
        }
      }

      const storageKey = `wqst_create_case_key_${companyId}_${productId}_${serviceType}`
      const detail = await createCaseDraft({ companyId, productId, serviceType, caseMonth: '' }, operationKey(storageKey))
      uni.removeStorageSync(storageKey)
      if (!detail.id) throw new Error('创建咨询草稿后未返回报单编号')
      return await loadCase(detail.id, false)
    } catch (cause) {
      error.value = errorMessage(cause, '准备咨询草稿失败')
      throw cause
    } finally {
      loading.value = false
    }
  }

  async function refreshCase() {
    if (!caseDetail.value?.id) throw new Error('尚未选择报单')
    return loadCase(caseDetail.value.id, false)
  }

  async function saveInvoiceInfo(request: InvoiceRequest) {
    if (!caseDetail.value?.id) throw new Error('尚未选择报单')
    await saveInvoice(caseDetail.value.id, request)
    await refreshCase()
  }

  async function saveSimpleForm(request: SimpleRequest) {
    if (!caseDetail.value?.id) throw new Error('尚未选择报单')
    caseDetail.value = await saveSimpleInfo(caseDetail.value.id, request)
    return caseDetail.value
  }

  async function saveTextMaterial(materialId: number, value: string) {
    if (!caseDetail.value?.id) throw new Error('尚未选择报单')
    busyMaterialId.value = materialId
    try {
      await saveMaterialText(caseDetail.value.id, materialId, value)
      await refreshCase()
    } finally {
      busyMaterialId.value = undefined
    }
  }

  async function uploadFile(materialId: number, filePath: string) {
    if (!caseDetail.value?.id) throw new Error('尚未选择报单')
    busyMaterialId.value = materialId
    try {
      await uploadCaseMaterial(caseDetail.value.id, materialId, filePath)
      filesByMaterial.value[materialId] = await getMaterialFiles(materialId)
      caseDetail.value = await getCaseDetail(caseDetail.value.id)
    } finally {
      busyMaterialId.value = undefined
    }
  }

  async function deleteFile(materialId: number, fileId: number) {
    busyMaterialId.value = materialId
    try {
      await deleteMaterialFile(fileId)
      filesByMaterial.value[materialId] = await getMaterialFiles(materialId)
      if (caseDetail.value?.id) caseDetail.value = await getCaseDetail(caseDetail.value.id)
    } finally {
      busyMaterialId.value = undefined
    }
  }

  async function submitCurrentCase() {
    if (!caseDetail.value?.id) throw new Error('尚未选择报单')
    submitting.value = true
    try {
      await refreshCase()
      const storageKey = `wqst_submit_case_key_${caseDetail.value.id}`
      caseDetail.value = await submitCase(caseDetail.value.id, operationKey(storageKey))
      uni.removeStorageSync(storageKey)
      uni.removeStorageSync(CURRENT_DRAFT_STORAGE_KEY)
      return caseDetail.value
    } finally {
      submitting.value = false
    }
  }

  function reset() {
    caseDetail.value = undefined
    template.value = undefined
    filesByMaterial.value = {}
    loading.value = false
    submitting.value = false
    busyMaterialId.value = undefined
    error.value = ''
    uni.removeStorageSync(CURRENT_DRAFT_STORAGE_KEY)
    const keys = uni.getStorageInfoSync().keys || []
    keys.filter((key) => key.startsWith('wqst_create_case_key_') || key.startsWith('wqst_submit_case_key_'))
      .forEach((key) => uni.removeStorageSync(key))
  }

  return {
    caseDetail,
    template,
    filesByMaterial,
    materials,
    loading,
    submitting,
    busyMaterialId,
    error,
    isEditable,
    completedCount,
    totalCount,
    completionPercent,
    prepareTradeDraft,
    prepareSimpleDraft,
    loadCase,
    refreshCase,
    saveInvoiceInfo,
    saveSimpleForm,
    saveTextMaterial,
    uploadFile,
    deleteFile,
    submitCurrentCase,
    reset,
  }
})
