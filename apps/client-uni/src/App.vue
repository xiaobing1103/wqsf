<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { CLIENT_AUTH_EXPIRED_EVENT } from './api/generated-client'
import { useAuthStore } from './stores/auth'
import { useCapabilitiesStore } from './stores/capabilities'

onLaunch(() => {
  void useCapabilitiesStore().load()
  const authStore = useAuthStore()
  uni.$on(CLIENT_AUTH_EXPIRED_EVENT, authStore.handleExpiredSession)
  void authStore.restoreSession()
})
</script>

<style>
page {
  --color-primary: #164f50;
  --color-primary-light: #26706f;
  --color-gold: #efbd5b;
  --color-background: #f4f7f7;
  --color-surface: #ffffff;
  --color-text: #142729;
  --color-muted: #718083;
  --color-border: #dfe7e7;
  min-height: 100%;
  background: var(--color-background);
  color: var(--color-text);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 28rpx;
}

view,
text,
button {
  box-sizing: border-box;
}

button::after {
  border: 0;
}

.safe-page {
  min-height: 100vh;
  padding-bottom: calc(132rpx + env(safe-area-inset-bottom));
}

.state-panel {
  margin: 24rpx 32rpx;
  padding: 30rpx;
  border: 1rpx solid var(--color-border);
  border-radius: 24rpx;
  background: var(--color-surface);
  text-align: center;
}

.state-title,
.state-copy {
  display: block;
}

.state-title {
  font-size: 30rpx;
  font-weight: 700;
}

.state-copy {
  margin-top: 10rpx;
  color: var(--color-muted);
  font-size: 24rpx;
  line-height: 1.6;
}

.state-action {
  min-width: 184rpx;
  height: 72rpx;
  margin-top: 22rpx;
  padding: 0 30rpx;
  border-radius: 20rpx;
  background: var(--color-primary);
  color: #fff;
  font-size: 26rpx;
  line-height: 72rpx;
}
</style>
