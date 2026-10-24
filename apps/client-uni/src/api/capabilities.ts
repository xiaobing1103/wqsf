import { capabilityCapabilities } from './generated/endpoints'
import { requireData } from './response'

export async function getClientCapabilities() {
  return requireData(await capabilityCapabilities(), '读取功能状态失败')
}
