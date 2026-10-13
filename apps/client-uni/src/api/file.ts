import { fileDelete, fileList } from './generated/endpoints'
import type { FileView } from './generated/models'
import { requireData } from './response'
import { uploadMaterialFile } from './upload'

export function getMaterialFiles(materialId: number) {
  return fileList({ materialId }).then((response) => requireData<FileView[]>(response, '读取附件失败'))
}

export function uploadCaseMaterial(caseId: number, materialId: number, filePath: string) {
  return uploadMaterialFile({ caseId, materialId, filePath })
}

export async function deleteMaterialFile(fileId: number) {
  await fileDelete(fileId)
}
