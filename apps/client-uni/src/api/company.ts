import { companyMine } from './generated/endpoints'
import type { CompanyView } from './generated/models'
import { requireData } from './response'

export function getMyCompanies() {
  return companyMine().then((response) => requireData<CompanyView[]>(response, '读取企业身份失败'))
}
