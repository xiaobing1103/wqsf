<script setup lang="ts">
import { ref } from 'vue'

const activeTab = ref('home')
const toast = ref('')

const showToast = (message: string) => {
  toast.value = message
  setTimeout(() => (toast.value = ''), 2200)
}

const go = (tab: string) => {
  activeTab.value = tab
  if (tab === 'case') showToast('报单页面已打开')
}
</script>

<template>
  <view class="app-shell">
    <view class="status-bar"><text>9:41</text><text>● ● ● ▰</text></view>

    <view v-if="activeTab === 'home'" class="page home-page">
      <view class="topbar">
        <view>
          <text class="eyebrow">WQ SERVICE · 企业服务</text>
          <text class="brand">万企商服通</text>
        </view>
        <view class="notification" @click="showToast('暂无新的通知')">⌁<view class="dot" /></view>
      </view>

      <view class="hero-card">
        <view class="hero-copy">
          <text class="hero-kicker">一站式业务资料协作</text>
          <text class="hero-title">让每一次报单，<text class="hero-accent">清晰可追踪</text></text>
          <text class="hero-desc">提交资料、补充材料、查看进度，办理过程一步到位。</text>
          <button class="hero-btn" @click="go('case')">开始报单 <text>→</text></button>
        </view>
        <view class="hero-orbit orbit-one" /><view class="hero-orbit orbit-two" />
        <view class="hero-file"><text class="file-mark">↗</text><text>资料</text><text class="file-state">已加密</text></view>
      </view>

      <view class="section-head"><view><text class="section-title">选择服务</text><text class="section-sub">按业务类型提交资料</text></view><text class="more" @click="showToast('更多服务即将上线')">全部服务 ›</text></view>
      <view class="service-grid">
        <view class="service-card service-main" @click="go('case')"><view class="service-icon gold">↗</view><text class="service-name">贸易增量</text><text class="service-desc">11 项资料清单</text><view class="service-arrow">→</view></view>
        <view class="service-card" @click="showToast('知识产权服务即将上线')"><view class="service-icon purple">◇</view><text class="service-name">知识产权</text><text class="service-desc">基础信息报单</text><view class="service-arrow">→</view></view>
        <view class="service-card" @click="showToast('资质申报服务即将上线')"><view class="service-icon blue">✦</view><text class="service-name">资质申报</text><text class="service-desc">专人跟进办理</text><view class="service-arrow">→</view></view>
      </view>

      <view class="section-head compact"><view><text class="section-title">当前报单</text><text class="section-sub">你的业务进度</text></view><text class="more" @click="go('case')">查看全部 ›</text></view>
      <view class="case-card" @click="go('case')"><view class="case-left"><view class="case-icon">↗</view><view><text class="case-name">贸易增量服务</text><text class="case-no">BD20260921001 · 09月21日提交</text></view></view><view class="case-right"><text class="case-status">待补资料</text><text class="case-progress">8 / 11</text></view></view>
    </view>

    <view v-else-if="activeTab === 'case'" class="page case-page">
      <view class="inner-topbar"><text class="back" @click="go('home')">‹</text><text class="inner-title">贸易增量报单</text><text class="help" @click="showToast('客服将在工作时间联系你')">帮助</text></view>
      <view class="stepper"><view class="step done"><text>1</text><label>选择服务</label></view><view class="step active"><text>2</text><label>提交资料</label></view><view class="step"><text>3</text><label>审核处理</label></view></view>
      <view class="form-intro"><text class="form-title">完善企业资料</text><text class="form-desc">请按清单准备资料，支持图片、PDF、视频和文字。</text></view>
      <view class="completion"><view><text>资料完成度</text><text class="completion-value">8 <small>/ 11</small></text></view><view class="ring"><text>73%</text></view></view>
      <view class="group-label">基础信息 <text>必填</text></view>
      <view class="field-card"><view class="field-icon red">▣</view><view class="field-copy"><text class="field-title">营业执照</text><text class="field-desc">上传清晰的营业执照图片</text></view><text class="field-state complete">已完成</text></view>
      <view class="field-card" @click="showToast('开票信息编辑页已打开')"><view class="field-icon orange">＃</view><view class="field-copy"><text class="field-title">开票信息</text><text class="field-desc">公司名称、税号、开户行等 8 项</text></view><text class="field-state complete">已填写</text></view>
      <view class="group-label">证明与额度 <text>5 项</text></view>
      <view class="field-card"><view class="field-icon cyan">↑</view><view class="field-copy"><text class="field-title">法人身份证扫描件</text><text class="field-desc">需盖公章，支持 JPG / PNG / PDF</text></view><text class="field-state complete">已上传</text></view>
      <view class="field-card attention" @click="showToast('请上传税务局开票额度截图')"><view class="field-icon pink">!</view><view class="field-copy"><text class="field-title">税务局开票额度截图</text><text class="field-desc">右上角公司名称，右下角实时日期</text></view><text class="field-state pending">待补充</text></view>
      <view class="field-card"><view class="field-icon green">↑</view><view class="field-copy"><text class="field-title">网银限额与余额截图</text><text class="field-desc">余额需大于 1000 元</text></view><text class="field-state pending">待补充</text></view>
      <button class="primary-btn" @click="showToast('已保存，等待后台审核')">保存并提交审核 <text>→</text></button>
    </view>

    <view v-else class="page profile-page">
      <view class="topbar"><view><text class="eyebrow">ACCOUNT</text><text class="brand">我的资料</text></view><view class="avatar">刘</view></view>
      <view class="profile-banner"><view class="avatar large">刘</view><view><text class="profile-name">刘立</text><text class="profile-sub">已绑定 3 个企业账号</text></view><text class="profile-arrow">›</text></view>
      <view class="menu-card"><view class="menu-row" @click="showToast('企业资料管理')"><text>▣　企业资料</text><text>›</text></view><view class="menu-row" @click="showToast('通知设置')"><text>◌　消息通知</text><text>›</text></view><view class="menu-row" @click="showToast('联系客服')"><text>◉　联系客服</text><text>›</text></view></view>
      <view class="privacy-note">你的资料将被安全加密保存，仅用于已授权的业务办理。</view>
    </view>

    <view class="tabbar"><view :class="['tab', activeTab === 'home' && 'selected']" @click="go('home')"><text class="tab-icon">⌂</text><text>首页</text></view><view :class="['tab', activeTab === 'case' && 'selected']" @click="go('case')"><text class="tab-icon">＋</text><text>报单</text></view><view :class="['tab', activeTab === 'profile' && 'selected']" @click="go('profile')"><text class="tab-icon">◎</text><text>我的</text></view></view>
    <view v-if="toast" class="toast">{{ toast }}</view>
  </view>
</template>

<style>
page { background: #f5f6f8; color: #182229; font-family: -apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif; }
* { box-sizing: border-box; }
.app-shell { min-height: 100vh; background: #f5f6f8; padding-bottom: 76px; }
.status-bar { height: 28px; padding: 8px 24px 0; display:flex; justify-content:space-between; font-size:10px; color:#5d6b75; }
.page { padding: 20px 20px 30px; }
.topbar,.inner-topbar { display:flex; justify-content:space-between; align-items:center; }
.eyebrow { display:block; font-size:10px; letter-spacing:2px; color:#849099; margin-bottom:5px; }
.brand { display:block; font-size:25px; font-weight:800; letter-spacing:1px; }
.notification { width:38px; height:38px; border-radius:14px; background:#fff; box-shadow:0 7px 24px rgba(24,34,41,.06); display:flex; justify-content:center; align-items:center; font-size:22px; position:relative; }
.dot { width:7px; height:7px; background:#fb6477; border:2px solid white; border-radius:50%; position:absolute; right:8px; top:7px; }
.hero-card { margin-top:24px; min-height:208px; border-radius:24px; padding:23px; color:#fff; position:relative; overflow:hidden; background:linear-gradient(135deg,#14272e 0%,#1f4b50 50%,#d29d4a 180%); box-shadow:0 16px 30px rgba(20,50,56,.18); }
.hero-copy { position:relative; z-index:2; width:75%; }.hero-kicker { font-size:11px; color:#b7d8d3; display:block; margin-bottom:9px; }.hero-title { font-size:25px; font-weight:700; display:block; line-height:1.25; }.hero-accent { color:#f0c56e; }.hero-desc { display:block; margin-top:10px; font-size:11px; line-height:1.7; color:#d7e4e1; }.hero-btn { margin-top:15px; border:0; background:#f1c675; color:#25373a; font-size:12px; font-weight:700; border-radius:10px; padding:10px 14px; }.hero-btn text { margin-left:8px; }.hero-orbit { position:absolute; border:1px solid rgba(255,255,255,.2); border-radius:50%; }.orbit-one { width:190px; height:190px; right:-45px; top:23px; }.orbit-two { width:140px; height:140px; right:-20px; top:48px; }.hero-file { position:absolute; right:21px; bottom:22px; width:74px; height:83px; background:rgba(255,255,255,.12); border:1px solid rgba(255,255,255,.2); border-radius:12px; padding:11px; display:flex; flex-direction:column; gap:3px; font-size:11px; }.file-mark { font-size:25px; color:#f4c971; }.file-state { color:#b8d8d4; font-size:9px; }
.section-head { margin:26px 0 13px; display:flex; justify-content:space-between; align-items:end; }.section-head.compact { margin-top:29px; }.section-title { display:block; font-size:17px; font-weight:800; }.section-sub { display:block; color:#8b979e; font-size:11px; margin-top:4px; }.more { color:#647b7a; font-size:11px; }
.service-grid { display:grid; grid-template-columns:1.3fr 1fr; gap:10px; }.service-card { background:#fff; border-radius:17px; min-height:118px; padding:14px; position:relative; box-shadow:0 7px 20px rgba(31,48,54,.04); }.service-main { grid-row:span 2; min-height:246px; color:#fff; background:linear-gradient(145deg,#27686b,#17474d); }.service-icon { width:31px; height:31px; border-radius:10px; display:flex; align-items:center; justify-content:center; font-size:20px; margin-bottom:20px; }.gold { background:#f4ca70; color:#254b4c; }.purple { background:#ede6ff; color:#6d55bf; }.blue { background:#e1f1f4; color:#428793; }.service-name { display:block; font-weight:700; font-size:14px; }.service-desc { display:block; font-size:10px; color:#93a0a5; margin-top:5px; }.service-main .service-desc { color:#c4dedb; }.service-arrow { position:absolute; right:15px; bottom:14px; font-size:17px; color:#93a0a5; }.service-main .service-arrow { color:#f4ca70; }
.case-card { background:#fff; border-radius:16px; padding:16px; display:flex; justify-content:space-between; align-items:center; box-shadow:0 7px 20px rgba(31,48,54,.04); }.case-left { display:flex; align-items:center; gap:12px; }.case-icon { width:37px; height:37px; border-radius:12px; background:#fff1d7; color:#c78b2c; display:flex; justify-content:center; align-items:center; font-size:21px; }.case-name,.case-no { display:block; }.case-name { font-weight:700; font-size:12px; }.case-no { color:#97a0a5; font-size:9px; margin-top:4px; }.case-right { text-align:right; }.case-status { display:block; color:#cf8b25; font-size:10px; background:#fff4df; border-radius:6px; padding:4px 6px; }.case-progress { color:#829097; font-size:10px; margin-top:6px; display:block; }
.inner-topbar { margin: 3px -2px 26px; }.back { font-size:30px; color:#5e7177; }.inner-title { font-size:18px; font-weight:800; }.help { color:#71908d; font-size:11px; }.stepper { display:flex; justify-content:space-between; position:relative; margin:0 10px 25px; }.stepper:before { content:""; position:absolute; left:11%; right:11%; top:12px; height:1px; background:#d9dfdf; }.step { position:relative; z-index:1; text-align:center; color:#9aa5a8; font-size:10px; }.step text { display:flex; align-items:center; justify-content:center; width:25px; height:25px; margin:0 auto 7px; border-radius:50%; background:#fff; border:1px solid #d4dcdc; }.step.done,.step.active { color:#3e7775; }.step.done text,.step.active text { color:#fff; border-color:#3c7b78; background:#3c7b78; }.form-title { display:block; font-size:22px; font-weight:800; }.form-desc { display:block; color:#849095; font-size:11px; margin-top:6px; }.completion { display:flex; justify-content:space-between; align-items:center; margin:18px 0; background:#eef6f4; border-radius:16px; padding:14px 17px; color:#58817e; font-size:11px; }.completion-value { display:block; font-size:21px; color:#1f5b5b; font-weight:800; margin-top:3px; }.completion-value small { font-size:11px; color:#91a3a4; font-weight:400; }.ring { width:49px; height:49px; border:5px solid #c4dfd7; border-top-color:#39837c; border-radius:50%; display:flex; align-items:center; justify-content:center; color:#28716e; font-size:10px; font-weight:700; }.group-label { font-size:12px; color:#526269; font-weight:700; margin:18px 0 9px; }.group-label text { margin-left:4px; color:#9ea7aa; font-size:10px; font-weight:400; }.field-card { display:flex; align-items:center; background:#fff; border-radius:15px; padding:13px; margin-bottom:9px; box-shadow:0 6px 17px rgba(31,48,54,.035); }.field-icon { width:32px; height:32px; border-radius:10px; display:flex; align-items:center; justify-content:center; margin-right:11px; font-weight:800; }.red { color:#d66465; background:#ffeded; }.orange { color:#c4872d; background:#fff3dc; }.cyan { color:#4e9da1; background:#e5f6f4; }.pink { color:#d35d80; background:#ffedf3; }.green { color:#4a9b7b; background:#e3f7ed; }.field-copy { flex:1; }.field-title { display:block; font-size:12px; font-weight:700; }.field-desc { display:block; color:#99a4a8; font-size:9px; margin-top:4px; }.field-state { font-size:9px; padding:4px 6px; border-radius:5px; }.complete { color:#3e8a6c; background:#e7f6ee; }.pending { color:#cf8a35; background:#fff5e2; }.attention { border:1px solid #f6d8a5; }.primary-btn { margin-top:22px; width:100%; height:45px; border:0; border-radius:13px; background:#276b6c; color:#fff; font-size:13px; font-weight:700; }.primary-btn text { margin-left:10px; }
.avatar { width:36px; height:36px; border-radius:13px; background:#d9ece8; display:flex; justify-content:center; align-items:center; color:#34746e; font-weight:800; }.avatar.large { width:52px; height:52px; border-radius:18px; font-size:18px; }.profile-banner { margin-top:25px; padding:18px; border-radius:19px; background:linear-gradient(135deg,#20595c,#418781); color:#fff; display:flex; align-items:center; gap:13px; }.profile-name,.profile-sub { display:block; }.profile-name { font-size:16px; font-weight:800; }.profile-sub { font-size:10px; color:#d0e6df; margin-top:4px; }.profile-arrow { margin-left:auto; font-size:25px; }.menu-card { margin-top:16px; border-radius:17px; background:#fff; padding:0 16px; }.menu-row { display:flex; justify-content:space-between; padding:17px 0; border-bottom:1px solid #eff1f1; color:#43545a; font-size:12px; }.menu-row:last-child { border:0; }.privacy-note { color:#9ca7aa; font-size:10px; line-height:1.7; margin:17px 5px; }.tabbar { height:66px; position:fixed; left:0; right:0; bottom:0; z-index:20; background:rgba(255,255,255,.96); border-top:1px solid #e7ebeb; display:flex; justify-content:space-around; padding-top:9px; }.tab { color:#a1aaac; text-align:center; font-size:10px; }.tab-icon { display:block; font-size:21px; line-height:22px; margin-bottom:3px; }.tab.selected { color:#276b6c; font-weight:700; }.toast { position:fixed; bottom:90px; left:50%; transform:translateX(-50%); background:rgba(25,38,42,.9); color:#fff; padding:9px 14px; border-radius:9px; font-size:11px; z-index:30; white-space:nowrap; }
</style>
