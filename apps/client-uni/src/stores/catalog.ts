import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { ModuleView, ProductView } from '../api/generated/models'
import { getPublishedCatalog } from '../api/product'
import { errorMessage } from '../api/response'
import { useCapabilitiesStore } from './capabilities'

export const useCatalogStore = defineStore('client-catalog', () => {
  const capabilities = useCapabilitiesStore()
  const modules = ref<ModuleView[]>([])
  const loading = ref(false)
  const error = ref('')
  const products = computed<ProductView[]>(() => modules.value
    .filter(item => capabilities.serviceEnabled(item.moduleCode))
    .flatMap((item) => item.products || []))

  async function loadCatalog(force = false) {
    if (!force && products.value.length) return
    loading.value = true
    error.value = ''
    try {
      await capabilities.load()
      if (capabilities.error) throw new Error(capabilities.error)
      modules.value = await getPublishedCatalog()
    } catch (cause) {
      error.value = errorMessage(cause, '读取服务目录失败')
      throw cause
    } finally {
      loading.value = false
    }
  }

  function reset() {
    modules.value = []
    loading.value = false
    error.value = ''
  }

  return { modules, products, loading, error, loadCatalog, reset }
})
