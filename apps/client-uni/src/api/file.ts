import { fileDelete, fileList, filePreview } from './generated/endpoints'
import type { FileView, PreviewView } from './generated/models'
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

export async function previewMaterialFile(file: FileView) {
  if (!file.id) throw new Error('缺少文件编号')
  const data = requireData<PreviewView>(await filePreview(file.id), '读取预览地址失败')
  if (!data.previewUrl) throw new Error('未返回文件预览地址')
  if (/^(jpg|jpeg|png|gif|webp)$/i.test(file.extension || '') || file.contentType?.startsWith('image/')) {
    await new Promise<void>((resolve, reject) => uni.previewImage({ urls: [data.previewUrl!], success: () => resolve(), fail: (error) => reject(new Error(error.errMsg)) }))
    return
  }
  // #ifdef H5
  const link = document.createElement('a')
  link.href = data.previewUrl
  link.target = '_blank'
  link.rel = 'noopener'
  link.click()
  return
  // #endif
  // #ifdef MP-WEIXIN
  const path = await new Promise<string>((resolve, reject) => uni.downloadFile({ url: data.previewUrl!, success: (response) => response.statusCode === 200 ? resolve(response.tempFilePath) : reject(new Error('文件下载失败')), fail: (error) => reject(new Error(error.errMsg)) }))
  await new Promise<void>((resolve, reject) => uni.openDocument({ filePath: path, fileType: file.extension as 'pdf', showMenu: true, success: () => resolve(), fail: (error) => reject(new Error(error.errMsg || '当前文件类型不支持在线预览')) }))
  // #endif
}
