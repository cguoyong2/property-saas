import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import path from 'node:path'

export default defineConfig({
  define: {
    __UNI_FEATURE_WX__: 'false',
    __UNI_FEATURE_WXS__: 'false',
    __UNI_FEATURE_RPX__: 'true',
    __UNI_FEATURE_PROMISE__: 'true',
    __UNI_FEATURE_LONGPRESS__: 'false',
    __UNI_FEATURE_I18N_EN__: 'false',
    __UNI_FEATURE_I18N_ES__: 'false',
    __UNI_FEATURE_I18N_FR__: 'false',
    __UNI_FEATURE_I18N_ZH_HANS__: 'true',
    __UNI_FEATURE_I18N_ZH_HANT__: 'false',
    __UNI_FEATURE_UNI_CLOUD__: 'false',
    __UNI_FEATURE_I18N_LOCALE__: 'false',
    __UNI_FEATURE_NVUE__: 'false',
    __UNI_FEATURE_ROUTER_MODE__: JSON.stringify('hash'),
    __UNI_FEATURE_PAGES__: 'true',
    __UNI_FEATURE_TABBAR__: 'false',
    __UNI_FEATURE_TABBAR_MIDBUTTON__: 'false',
    __UNI_FEATURE_TOPWINDOW__: 'false',
    __UNI_FEATURE_LEFTWINDOW__: 'false',
    __UNI_FEATURE_RIGHTWINDOW__: 'false',
    __UNI_FEATURE_RESPONSIVE__: 'false',
    __UNI_FEATURE_NAVIGATIONBAR__: 'false',
    __UNI_FEATURE_PULL_DOWN_REFRESH__: 'false',
    __UNI_FEATURE_NAVIGATIONBAR_BUTTONS__: 'false',
    __UNI_FEATURE_NAVIGATIONBAR_SEARCHINPUT__: 'false',
    __UNI_FEATURE_NAVIGATIONBAR_TRANSPARENT__: 'false',
    __UNI_FEATURE_VIRTUAL_HOST__: 'false',
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      vue: '@dcloudio/uni-h5-vue'
    }
  },
  plugins: [uni()],
})
