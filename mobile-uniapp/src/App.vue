<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // TASK-001 only initializes the app shell; auth is implemented in TASK-021.
})
</script>

<style>
page {
  min-height: 100%;
  background: #f5f7fb;
  color: #1f2937;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
}
</style>
