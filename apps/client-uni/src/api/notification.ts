import { notificationMine, notificationRead } from './generated/endpoints'
import type { PageResponseNotificationEntity } from './generated/models'
import { requireData, requireSuccess } from './response'

export function getMyNotifications(page = 1, pageSize = 10) {
  return notificationMine({ page, pageSize })
    .then((response) => requireData<PageResponseNotificationEntity>(response, '读取消息通知失败'))
}

export function markNotificationRead(notificationId: number) {
  return notificationRead(notificationId)
    .then((response) => requireSuccess(response, '标记通知已读失败'))
}
