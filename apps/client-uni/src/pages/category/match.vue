<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '../../components/AppIcon.vue'
import BottomNav from '../../components/BottomNav.vue'
import { recognizeBusinessLicense, recommendCategories } from '../../api/category-match'
import { errorMessage } from '../../api/response'
import type { CategoryMatchView } from '../../api/generated/models'
import { useAuthStore } from '../../stores/auth'
import { useCapabilitiesStore } from '../../stores/capabilities'
const capabilities = useCapabilitiesStore()

const authStore = useAuthStore()
const imagePath = ref('')
const companyName = ref('')
const businessScope = ref('')
const establishedDate = ref('')
const registeredCapital = ref('')
const recognizing = ref(false)
const matching = ref(false)
const ocrNotice = ref('')
const pageError = ref('')
const result = ref<CategoryMatchView | null>(null)

const canMatch = computed(() => capabilities.flags.categoryMatchEnabled && businessScope.value.trim().length >= 2 && !matching.value)
const resultSummary = computed(() => {
  if (!result.value) return ''
  return `${result.value.industryCount || 0} 个行业 · ${result.value.categoryCount || 0} 个品类`
})

function chooseLicense() {
  if (recognizing.value || !capabilities.flags.licenseOcrEnabled) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (selection) => {
      const path = selection.tempFilePaths[0]
      const selectedFile = Array.isArray(selection.tempFiles) ? selection.tempFiles[0] : selection.tempFiles
      const size = selectedFile && 'size' in selectedFile ? Number(selectedFile.size) : 0
      if (!path) return
      if (size > 10 * 1024 * 1024) {
        uni.showToast({ title: '图片不能超过 10MB', icon: 'none' })
        return
      }
      imagePath.value = path
      result.value = null
      void recognize(path)
    },
  })
}

async function recognize(path: string) {
  recognizing.value = true
  ocrNotice.value = ''
  try {
    const data = await recognizeBusinessLicense(path)
    companyName.value = data.companyName || companyName.value
    businessScope.value = data.businessScope || businessScope.value
    establishedDate.value = data.establishedDate || ''
    registeredCapital.value = data.registeredCapital || ''
    ocrNotice.value = '已自动识别，请核对后开始匹配'
  } catch (cause) {
    ocrNotice.value = `${errorMessage(cause, '自动识别失败')}，可在下方手工填写后继续匹配。`
  } finally {
    recognizing.value = false
  }
}

function previewLicense() {
  if (imagePath.value) uni.previewImage({ current: imagePath.value, urls: [imagePath.value] })
}

async function startMatch() {
  if (!canMatch.value) {
    uni.showToast({ title: '请填写营业范围或主营产品', icon: 'none' })
    return
  }
  matching.value = true
  pageError.value = ''
  try {
    result.value = await recommendCategories({
      companyName: companyName.value.trim(),
      businessScope: businessScope.value.trim(),
    })
  } catch (cause) {
    pageError.value = errorMessage(cause, '品类匹配失败')
  } finally {
    matching.value = false
  }
}

function openServices() {
  uni.navigateTo({ url: '/pages/service/select' })
}

onShow(() => {
  void capabilities.load()
  void authStore.ensureSession().catch((cause) => {
    pageError.value = errorMessage(cause, '登录状态校验失败')
  })
})
</script>

<template>
  <view class="safe-page match-page">
    <view v-if="!capabilities.flags.categoryMatchEnabled" class="state-panel"><text class="state-title">品类参考功能暂未开放</text><text class="state-copy">{{ capabilities.error || '已有报单可继续在首页或我的页面查看。' }}</text></view>
    <template v-else>
    <view class="intro-card">
      <view class="intro-orbit intro-orbit-large" />
      <view class="intro-orbit intro-orbit-small" />
      <view class="intro-icon"><AppIcon type="scan" :size="28" /></view>
      <text class="intro-kicker">智能经营范围分析</text>
      <text class="intro-title">经营范围关键词，匹配品类参考</text>
      <text class="intro-copy">识别企业名称与经营范围，按业务关键词生成推荐清单；识别结果支持手工校正。</text>
    </view>

    <view class="form-card">
      <view class="card-heading">
        <view><text class="card-title">营业执照 / 经营范围</text><text class="required-copy">支持 JPG、JPEG、PNG，最大 10MB</text></view>
        <text class="required-mark">必填</text>
      </view>

      <view v-if="!capabilities.flags.licenseOcrEnabled" class="recognize-notice">图片识别暂未开放，请手工填写经营范围进行参考匹配。</view>
      <button v-else-if="!imagePath" class="upload-zone" :disabled="recognizing" @click="chooseLicense">
        <view class="upload-icon"><AppIcon type="camera-filled" :size="31" /></view>
        <text class="upload-title">拍照或从相册上传</text>
        <text class="upload-copy">请保证证照边缘完整、文字清晰、无明显反光</text>
      </button>
      <view v-else class="license-preview">
        <image :src="imagePath" mode="aspectFill" @click="previewLicense" />
        <button class="preview-label" @click="previewLicense"><AppIcon type="image" :size="17" /> 查看大图</button>
        <button class="reupload-button" :disabled="recognizing" @click="chooseLicense">
          <AppIcon type="refresh" :size="16" />{{ recognizing ? '识别中…' : '重新上传' }}
        </button>
      </view>

      <view v-if="recognizing" class="recognize-state"><view class="spinner" /><text>正在安全识别营业执照…</text></view>
      <view v-else-if="ocrNotice" class="recognize-notice" :class="{ success: businessScope }">
        <AppIcon :type="businessScope ? 'checkbox-filled' : 'info-filled'" :size="18" />
        <text>{{ ocrNotice }}</text>
      </view>

      <view v-if="establishedDate || registeredCapital" class="license-meta">
        <text v-if="establishedDate">成立日期：{{ establishedDate }}</text>
        <text v-if="registeredCapital">注册资本：{{ registeredCapital }}</text>
      </view>

      <view class="field-group">
        <text class="field-label">公司名称 <small>识别后自动回填，可修改</small></text>
        <input v-model="companyName" class="text-input" maxlength="128" placeholder="请输入公司全称（选填）" />
      </view>
      <view class="field-group">
        <text class="field-label">经营范围 / 主营产品 <small>请保留完整关键词</small></text>
        <textarea
          v-model="businessScope"
          class="scope-input"
          maxlength="5000"
          placeholder="例如：软件开发、信息技术咨询服务、计算机软硬件销售……"
        />
        <text class="word-count">{{ businessScope.length }} / 5000</text>
      </view>
    </view>

    <button class="match-button" :disabled="!canMatch" @click="startMatch">
      <AppIcon type="search" :size="21" />{{ matching ? '正在匹配…' : '开始匹配品类' }}
    </button>
    <view class="privacy-tip"><AppIcon type="info-filled" :size="15" /><text>匹配仅为关键词参考，不代表取得经营许可或业务资质。</text></view>
    <view v-if="capabilities.flags.licenseOcrEnabled" class="privacy-tip"><AppIcon type="locked-filled" :size="15" /><text>图片发送至腾讯云 OCR 识别，本系统不写入报单和文件库；请先确认有权提交。</text></view>

    <view v-if="pageError" class="state-panel error-panel">
      <text class="state-title">暂时无法完成匹配</text><text class="state-copy">{{ pageError }}</text>
      <button class="state-action" @click="startMatch">重新匹配</button>
    </view>

    <view v-if="result" class="result-card">
      <view class="result-heading">
        <view><text class="result-kicker">可做品类推荐</text><text class="result-summary">{{ resultSummary }}</text></view>
        <view class="result-check"><AppIcon type="checkmarkempty" :size="20" /></view>
      </view>

      <view v-if="result.industries?.length" class="industry-list">
        <view v-for="industry in result.industries" :key="industry.industryCode" class="industry-card">
          <view class="industry-heading">
            <view class="industry-dot" /><text>{{ industry.industryName }}</text>
            <small>{{ industry.categories?.length || 0 }} 项</small>
          </view>
          <view class="category-list">
            <view v-for="category in industry.categories" :key="category.categoryCode" class="category-row">
              <view class="category-copy">
                <text class="category-name">{{ category.categoryName }}</text>
                <text class="category-reason">{{ category.reason }}</text>
              </view>
              <text class="score">{{ category.score }}%</text>
            </view>
          </view>
        </view>
        <button class="service-action" @click="openServices">选择服务并发起报单 <AppIcon type="right" :size="17" /></button>
      </view>
      <view v-else class="empty-result">
        <AppIcon type="info-filled" :size="28" />
        <text class="empty-title">暂未匹配到明确品类</text>
        <text class="empty-copy">请补充更完整的经营范围或主营产品关键词，也可以直接进入服务咨询。</text>
        <button @click="openServices">查看全部服务</button>
      </view>
    </view>

    </template>
    <BottomNav active="category" />
  </view>
</template>

<style scoped>
.match-page { padding-top: 24rpx; background: linear-gradient(180deg, #edf5f3 0, #f6f8f8 430rpx); }
.intro-card { position: relative; margin: 0 28rpx; padding: 34rpx 34rpx 38rpx; overflow: hidden; border-radius: 30rpx; background: linear-gradient(135deg, #123f42, #246866); color: #fff; box-shadow: 0 18rpx 36rpx rgba(22, 79, 80, 0.16); }
.intro-icon { display: flex; align-items: center; justify-content: center; width: 58rpx; height: 58rpx; border-radius: 18rpx; background: rgba(239, 189, 91, 0.18); color: var(--color-gold); }
.intro-kicker, .intro-title, .intro-copy { position: relative; z-index: 2; display: block; }
.intro-kicker { margin-top: 22rpx; color: var(--color-gold); font-size: 21rpx; font-weight: 700; letter-spacing: 2rpx; }
.intro-title { max-width: 570rpx; margin-top: 8rpx; font-size: 38rpx; font-weight: 800; line-height: 1.35; }
.intro-copy { max-width: 580rpx; margin-top: 14rpx; color: #c7d9d8; font-size: 22rpx; line-height: 1.65; }
.intro-orbit { position: absolute; border: 1rpx solid rgba(255, 255, 255, 0.12); border-radius: 50%; }
.intro-orbit-large { right: -140rpx; bottom: -250rpx; width: 500rpx; height: 500rpx; }
.intro-orbit-small { right: -16rpx; bottom: -150rpx; width: 320rpx; height: 320rpx; }
.form-card, .result-card { margin: 24rpx 28rpx 0; padding: 28rpx; border: 1rpx solid var(--color-border); border-radius: 28rpx; background: #fff; box-shadow: 0 12rpx 30rpx rgba(25, 67, 67, 0.06); }
.card-heading, .result-heading, .industry-heading { display: flex; align-items: center; justify-content: space-between; }
.card-title, .required-copy { display: block; }
.card-title { font-size: 29rpx; font-weight: 800; }
.required-copy { margin-top: 6rpx; color: var(--color-muted); font-size: 19rpx; }
.required-mark { padding: 6rpx 12rpx; border-radius: 99rpx; background: #fff1da; color: #8a5d12; font-size: 19rpx; font-weight: 700; }
.upload-zone { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 100%; height: 280rpx; margin: 24rpx 0 0; padding: 20rpx; border: 2rpx dashed #aac9c5; border-radius: 24rpx; background: #f5faf9; color: var(--color-text); line-height: 1.3; }
.upload-icon { display: flex; align-items: center; justify-content: center; width: 74rpx; height: 74rpx; border-radius: 24rpx; background: #dff0ed; color: var(--color-primary); }
.upload-title { margin-top: 18rpx; font-size: 26rpx; font-weight: 800; }
.upload-copy { max-width: 470rpx; margin-top: 9rpx; color: var(--color-muted); font-size: 20rpx; }
.license-preview { position: relative; height: 300rpx; margin-top: 24rpx; overflow: hidden; border-radius: 24rpx; background: #dfe7e7; }
.license-preview image { width: 100%; height: 100%; }
.preview-label, .reupload-button { position: absolute; display: flex; align-items: center; justify-content: center; gap: 7rpx; height: 52rpx; margin: 0; padding: 0 18rpx; border-radius: 16rpx; font-size: 20rpx; line-height: 1; }
.preview-label { bottom: 18rpx; left: 18rpx; background: rgba(16, 50, 52, 0.82); color: #fff; }
.reupload-button { right: 18rpx; bottom: 18rpx; background: var(--color-gold); color: #17383a; font-weight: 700; }
.recognize-state, .recognize-notice { display: flex; align-items: flex-start; gap: 12rpx; margin-top: 18rpx; padding: 17rpx 18rpx; border-radius: 18rpx; background: #fff4e5; color: #875c18; font-size: 21rpx; line-height: 1.5; }
.recognize-state { align-items: center; background: #edf7f5; color: var(--color-primary); }
.recognize-notice.success { background: #eaf6f1; color: #1d6856; }
.recognize-notice :deep(.app-icon) { margin-top: 3rpx; }
.spinner { width: 24rpx; height: 24rpx; border: 4rpx solid #c8dfdc; border-top-color: var(--color-primary); border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.license-meta { display: flex; flex-wrap: wrap; gap: 10rpx 22rpx; margin-top: 18rpx; color: var(--color-muted); font-size: 20rpx; }
.field-group { position: relative; min-width: 0; margin-top: 24rpx; }
.field-label { display: block; margin-bottom: 11rpx; font-size: 24rpx; font-weight: 700; }
.field-label small { margin-left: 8rpx; color: var(--color-muted); font-size: 19rpx; font-weight: 400; }
.text-input, .scope-input { width: 100%; min-width: 0; max-width: 100%; box-sizing: border-box; border: 1rpx solid #cfdcdc; border-radius: 20rpx; background: #f8fafa; color: var(--color-text); font-size: 24rpx; }
.text-input { height: 82rpx; padding: 0 22rpx; }
.scope-input { height: 230rpx; padding: 20rpx 22rpx 48rpx; line-height: 1.6; }
.word-count { position: absolute; right: 18rpx; bottom: 16rpx; color: #98a5a6; font-size: 18rpx; }
.match-button { display: flex; align-items: center; justify-content: center; gap: 12rpx; width: calc(100% - 56rpx); height: 88rpx; margin: 24rpx 28rpx 0; padding: 0; border-radius: 24rpx; background: linear-gradient(135deg, #164f50, #26706f); color: #fff; font-size: 27rpx; font-weight: 800; line-height: 1; box-shadow: 0 14rpx 28rpx rgba(22, 79, 80, 0.18); }
.match-button[disabled] { opacity: 0.46; box-shadow: none; }
.privacy-tip { display: flex; align-items: center; justify-content: center; gap: 7rpx; margin: 15rpx 28rpx 0; color: var(--color-muted); font-size: 19rpx; text-align: center; }
.error-panel { border-color: #efd6ca; background: #fff8f5; }
.result-card { margin-bottom: 22rpx; }
.result-kicker, .result-summary { display: block; }
.result-kicker { color: var(--color-primary); font-size: 29rpx; font-weight: 800; }
.result-summary { margin-top: 5rpx; color: var(--color-muted); font-size: 21rpx; }
.result-check { display: flex; align-items: center; justify-content: center; width: 52rpx; height: 52rpx; border-radius: 50%; background: #e2f1ee; color: var(--color-primary); }
.industry-list { margin-top: 22rpx; }
.industry-card { margin-top: 16rpx; padding: 20rpx; border: 1rpx solid #dce7e6; border-radius: 22rpx; background: #f8fafa; }
.industry-heading { justify-content: flex-start; font-size: 25rpx; font-weight: 800; }
.industry-heading small { margin-left: auto; color: var(--color-muted); font-size: 19rpx; font-weight: 500; }
.industry-dot { width: 13rpx; height: 13rpx; margin-right: 12rpx; border-radius: 50%; background: var(--color-gold); box-shadow: 0 0 0 7rpx #fff4dc; }
.category-list { margin-top: 12rpx; }
.category-row { display: flex; align-items: center; gap: 16rpx; padding: 17rpx 0; border-top: 1rpx solid #e5ecec; }
.category-copy { flex: 1; min-width: 0; }
.category-name, .category-reason { display: block; }
.category-name { font-size: 23rpx; font-weight: 700; }
.category-reason { margin-top: 6rpx; overflow: hidden; color: var(--color-muted); font-size: 19rpx; text-overflow: ellipsis; white-space: nowrap; }
.score { flex: none; padding: 6rpx 10rpx; border-radius: 12rpx; background: #e7f3f1; color: var(--color-primary); font-size: 19rpx; font-weight: 800; }
.service-action { display: flex; align-items: center; justify-content: center; gap: 8rpx; width: 100%; height: 72rpx; margin: 22rpx 0 0; padding: 0; border-radius: 20rpx; background: var(--color-gold); color: #17383a; font-size: 24rpx; font-weight: 800; line-height: 1; }
.empty-result { display: flex; flex-direction: column; align-items: center; padding: 34rpx 12rpx 12rpx; color: var(--color-muted); text-align: center; }
.empty-title { margin-top: 14rpx; color: var(--color-text); font-size: 26rpx; font-weight: 800; }
.empty-copy { max-width: 520rpx; margin-top: 10rpx; font-size: 21rpx; line-height: 1.6; }
.empty-result button { height: 64rpx; margin-top: 20rpx; padding: 0 28rpx; border-radius: 18rpx; background: var(--color-primary); color: #fff; font-size: 22rpx; line-height: 64rpx; }
</style>
