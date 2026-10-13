declare module '@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue' {
  import type { DefineComponent } from 'vue'

  const UniIcons: DefineComponent<{
    type: string
    size?: number | string
    color?: string
  }>

  export default UniIcons
}
