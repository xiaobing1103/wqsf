export type SelectedLocalFile = {
  path: string
  name: string
  size: number
}

type RawSelectedFile = {
  path?: string
  name?: string
  size?: number
}

function normalizeFiles(files: RawSelectedFile[]): SelectedLocalFile[] {
  return files
    .filter((file): file is Required<RawSelectedFile> => Boolean(file.path && file.name && typeof file.size === 'number'))
    .map((file) => ({ path: file.path, name: file.name, size: file.size }))
}

export function chooseLocalFiles(count: number, extensions: string[]) {
  // 微信桥接不能复制 Vue Proxy；平台参数必须是普通数组和基础类型。
  const plainExtensions = Array.from(extensions, (extension) => String(extension).trim().replace(/^\./, '').toLowerCase())
  const selectionCount = Math.min(100, Math.max(1, Math.floor(count)))
  return new Promise<SelectedLocalFile[]>((resolve, reject) => {
    let supported = false
    // #ifdef H5
    supported = true
    uni.chooseFile({
      count: selectionCount,
      extension: plainExtensions,
      success: (result) => resolve(normalizeFiles(result.tempFiles as RawSelectedFile[])),
      fail: (error) => reject(new Error(error.errMsg || '选择文件失败')),
    })
    // #endif
    // #ifdef MP-WEIXIN
    supported = true
    uni.chooseMessageFile({
      count: selectionCount,
      type: 'file',
      extension: plainExtensions,
      success: (result) => resolve(normalizeFiles(result.tempFiles as RawSelectedFile[])),
      fail: (error) => reject(new Error(error.errMsg || '选择文件失败')),
    })
    // #endif
    if (!supported) reject(new Error('当前平台暂不支持选择文件'))
  })
}

export function validateLocalFile(file: SelectedLocalFile, extensions: string[], maxFileSizeMb?: number) {
  const extension = file.name.includes('.') ? file.name.split('.').pop()?.toLowerCase() || '' : ''
  if (extensions.length && !extensions.map((item) => item.toLowerCase()).includes(extension)) {
    throw new Error(`仅支持 ${extensions.join('、')} 格式`)
  }
  if (maxFileSizeMb && file.size > maxFileSizeMb * 1024 * 1024) {
    throw new Error(`单个文件不能超过 ${maxFileSizeMb}MB`)
  }
}
