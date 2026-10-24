import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getClientCapabilities } from '../api/capabilities'

const keys = ['tradeApplicationEnabled', 'trademarkConsultationEnabled', 'qualificationConsultationEnabled',
  'categoryMatchEnabled', 'licenseOcrEnabled', 'sensitiveMaterialUploadEnabled', 'customerServiceEnabled'] as const
type CapabilityKey = typeof keys[number]
type Capabilities = Record<CapabilityKey, boolean>
const closed = (): Capabilities => Object.fromEntries(keys.map(key => [key, false])) as Capabilities

export const useCapabilitiesStore = defineStore('client-capabilities', () => {
  const flags = ref<Capabilities>(closed())
  const error = ref('')
  async function load() {
    // 每次访问入口重新读取；不持久化放开状态。请求失败关闭受控能力。
    try {
      const result = await getClientCapabilities()
      flags.value = Object.fromEntries(keys.map(key => [key, result[key] === true])) as Capabilities
      error.value = ''
    } catch {
      flags.value = closed()
      error.value = '暂时无法获取功能状态，已有报单仍可查看，请稍后重试。'
    }
  }
  function serviceEnabled(type?: string) {
    return type === 'TRADE_INCREMENT' ? flags.value.tradeApplicationEnabled
      : type === 'IP' ? flags.value.trademarkConsultationEnabled
      : type === 'QUALIFICATION' ? flags.value.qualificationConsultationEnabled : false
  }
  return { flags, error, load, serviceEnabled }
})
