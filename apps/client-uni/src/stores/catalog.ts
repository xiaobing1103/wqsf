import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { ModuleView, ProductView } from '../api/generated/models'
import { getPublishedCatalog } from '../api/product'
import { errorMessage } from '../api/response'

export const useCatalogStore = defineStore('client-catalog', () => {
  const modules = ref<ModuleView[]>([])
  const loading = ref(false)
  const error = ref('')
  const products = computed<ProductView[]>(() => modules.value.flatMap((item) => item.products || []))

  async function loadCatalog(force = false) {
    if (!force && products.value.length) return
    loading.value = true
    error.value = ''
    try {
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
