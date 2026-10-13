import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { CompanyView } from '../api/generated/models'
import { getMyCompanies } from '../api/company'
import { errorMessage } from '../api/response'

const COMPANY_STORAGE_KEY = 'wqst_client_company_id'

export const useCompanyStore = defineStore('client-company', () => {
  const companies = ref<CompanyView[]>([])
  const selectedId = ref<number>()
  const loading = ref(false)
  const error = ref('')
  const selectedCompany = computed(() => companies.value.find((item) => item.id === selectedId.value) || companies.value[0])

  async function loadCompanies() {
    loading.value = true
    error.value = ''
    try {
      companies.value = await getMyCompanies()
      const savedId = Number(uni.getStorageSync(COMPANY_STORAGE_KEY))
      const fallbackId = companies.value[0]?.id
      selectedId.value = companies.value.some((item) => item.id === savedId) ? savedId : fallbackId
      if (selectedId.value) uni.setStorageSync(COMPANY_STORAGE_KEY, selectedId.value)
    } catch (cause) {
      error.value = errorMessage(cause, '读取企业身份失败')
      throw cause
    } finally {
      loading.value = false
    }
  }

  function selectCompany(companyId: number) {
    if (!companies.value.some((item) => item.id === companyId)) return
    selectedId.value = companyId
    uni.setStorageSync(COMPANY_STORAGE_KEY, companyId)
  }

  function reset() {
    companies.value = []
    selectedId.value = undefined
    loading.value = false
    error.value = ''
    uni.removeStorageSync(COMPANY_STORAGE_KEY)
  }

  return { companies, selectedId, selectedCompany, loading, error, loadCompanies, selectCompany, reset }
})
