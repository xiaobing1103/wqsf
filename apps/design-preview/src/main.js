const qs = new URLSearchParams(location.search)
const screen = (qs.get('screen') || 'M01').toUpperCase()

const materialItems = [
  ['营业执照图片', '已上传', 'ok'],
  ['开票信息（8 项文字）', '已填写', 'ok'],
  ['法人身份证扫描件（盖公章）', '已上传', 'ok'],
  ['无欠税证明', '已上传', 'ok'],
  ['税务局开票额度截图', '待补充', 'warn'],
  ['网银限额截图', '待补充', 'warn'],
  ['网银余额大于 1000 元截图', '待补充', 'warn'],
  ['法人征信报告', '待审核', 'info'],
  ['企业征信报告', '待审核', 'info'],
  ['企业办公室照片及视频', '已上传', 'ok'],
  ['水母报告链接', '已填写', 'ok']
]

const cases = [
  ['BD20260921001', '示例科技有限公司', '贸易增量', '8 / 11', '待补资料', '刘立', '今天 10:18'],
  ['BD20260920008', '衡阳星河贸易有限公司', '贸易增量', '11 / 11', '处理中', '李敏', '昨天 16:42'],
  ['BD20260918003', '云智知识产权有限公司', '知识产权', '3 / 3', '待审核', '刘立', '09-18 11:06'],
  ['BD20260917011', '南方资质服务中心', '资质申报', '3 / 3', '已完成', '王珊', '09-17 09:45']
]

const statusClass = status => status.includes('完成') || status.includes('通过') || status.includes('填写') || status.includes('上传') ? 'success' : status.includes('处理') || status.includes('审核') ? 'info' : 'warning'
const badge = status => `<span class="badge ${statusClass(status)}">${status}</span>`

function phoneShell(title, content, tab = '') {
  return `<main class="mobile-screen">
    <header class="mobile-status"><span>9:41</span><span>● ● ● ▰</span></header>
    <section class="mobile-nav"><span class="nav-brand">${title}</span><span class="mini-menu">•••　◉</span></section>
    <section class="mobile-content">${content}</section>
    ${tab ? `<nav class="mobile-tabs"><span class="${tab === 'home' ? 'on' : ''}">⌂<small>首页</small></span><span class="${tab === 'case' ? 'on' : ''}">▤<small>报单</small></span><span class="${tab === 'mine' ? 'on' : ''}">◎<small>我的</small></span></nav>` : ''}
  </main>`
}

function M01() {
  return phoneShell('万企商服通', `
    <div class="mobile-greeting"><div><small>企业服务 · 资料协作</small><h1>上午好，刘先生</h1></div><button class="round">⌁<i /></button></div>
    <div class="hero"><small>一站式企业服务协作</small><h2>业务办理<br><em>清晰可追踪</em></h2><p>提交资料、补充材料、查看进度，一站完成。</p><button>开始报单　→</button><div class="hero-file">↗<b>资料</b><small>安全传输</small></div></div>
    <div class="mobile-section"><div><h3>选择服务</h3><p>按业务类型提交资料</p></div><a>全部服务 ›</a></div>
    <div class="service-cards"><article class="service-feature"><span>↗</span><h3>贸易增量</h3><p>11 项资料清单</p><b>→</b></article><div><article><span>◇</span><h3>知识产权</h3><p>基础信息报单</p></article><article><span>✦</span><h3>资质申报</h3><p>专人跟进办理</p></article></div></div>
    <div class="mobile-section compact"><div><h3>当前报单</h3><p>你的业务进度</p></div><a>查看全部 ›</a></div>
    <div class="current-case"><span class="case-logo">↗</span><div><b>贸易增量服务</b><small>BD20260921001 · 09月21日</small></div>${badge('待补资料')}<em>8 / 11</em></div>
  `, 'home')
}

function M02() {
  return phoneShell('选择服务', `
    <div class="back-title"><span>‹</span><div><small>STEP 01</small><h1>选择本次服务</h1></div></div>
    <div class="service-detail selected"><div class="detail-icon">↗</div><div><small>TRADE INCREMENT</small><h2>贸易增量</h2><p>适用于已合作企业提交月度业务材料，由专人审核与跟进。</p></div><i>✓</i></div>
    <div class="requirements"><div><span>资料清单</span><b>11 项</b></div><div><span>办理方式</span><b>线上提交</b></div><div><span>进度通知</span><b>实时查看</b></div></div>
    <div class="section-label">其他服务</div>
    <div class="service-choice"><span class="choice-icon violet">◇</span><div><h3>知识产权</h3><p>填写法人姓名、公司名称、联系电话</p></div><i>›</i></div>
    <div class="service-choice"><span class="choice-icon cyan">✦</span><div><h3>资质申报</h3><p>填写法人姓名、公司名称、联系电话</p></div><i>›</i></div>
    <div class="tip"><b>隐私提示</b><p>敏感资料仅用于约定业务办理，提交前需确认授权。</p></div>
    <button class="mobile-primary">确认并填写资料　→</button>
  `)
}

function M03() {
  const rows = materialItems.map((m, i) => `<div class="material-line"><span>${String(i + 1).padStart(2, '0')}</span><div><b>${m[0]}</b><small>${i === 4 ? '需包含公司名称及实时日期' : i === 5 || i === 6 ? '客户暂未上传' : '资料已保存'}</small></div><em class="${m[2]}">${m[1]}</em></div>`).join('')
  return phoneShell('贸易增量报单', `
    <div class="steps"><span class="done">1<small>选择服务</small></span><i></i><span class="active">2<small>提交资料</small></span><i></i><span>3<small>审核处理</small></span></div>
    <div class="form-heading"><div><small>TRADE INCREMENT</small><h1>资料提交清单</h1><p>请完整提交以下 11 项资料</p></div><div class="circle-progress"><b>73%</b><small>8/11</small></div></div>
    <div class="material-list compact-list">${rows}</div>
    <div class="sticky-actions"><button class="ghost">保存草稿</button><button class="mobile-primary inline">提交审核　→</button></div>
  `, 'case')
}

function M04() {
  return phoneShell('补充资料', `
    <div class="back-title slim"><span>‹</span><div><small>STEP 02</small><h1>开票信息与附件</h1></div></div>
    <div class="segmented"><b>开票信息</b><span>附件资料</span></div>
    <div class="form-grid"><label>公司名称<input value="示例科技有限公司" /></label><label>税号<input value="9143**********8X" /></label><label>联系地址<input value="湖南省衡阳市蒸湘区示例路 88 号" /></label><label>联系电话<input value="153 **** 3374" /></label><label>法定代表人<input value="赵先生" /></label><label>公司邮箱<input value="service@example.com" /></label><label>开户行网点<input value="示例银行衡阳分行" /></label><label>基本户账号<input value="**** **** **** 6628" /></label></div>
    <div class="upload-title"><b>附件资料</b><span>支持 JPG、PNG、PDF、MP4</span></div>
    <div class="upload-card"><span>↑</span><div><b>法人身份证扫描件（盖公章）</b><small>法人身份证扫描件.pdf · 2.4MB</small></div><em class="success">已上传</em></div>
    <div class="upload-card error"><span>!</span><div><b>税务局开票额度截图</b><small>上传失败：网络中断，请重新上传</small></div><button>重试</button></div>
    <div class="upload-rule">文件将进入私有存储，只有获得敏感资料权限的审核人员可查看。</div>
    <button class="mobile-primary">保存并继续　→</button>
  `)
}

function M05() {
  return phoneShell('企业服务报单', `
    <div class="back-title"><span>‹</span><div><small>SIMPLE APPLICATION</small><h1>快速提交需求</h1></div></div>
    <div class="simple-banner"><div><small>已选服务</small><h2>知识产权</h2><p>提交基础联系方式后，由服务人员进一步沟通材料。</p></div><span>◇</span></div>
    <div class="simple-switch"><b>知识产权</b><span>资质申报</span></div>
    <div class="large-form"><label>法人姓名 <em>*</em><input placeholder="请输入法人姓名" value="赵先生" /></label><label>公司名称 <em>*</em><input placeholder="请输入公司全称" value="示例科技有限公司" /></label><label>联系电话 <em>*</em><input placeholder="请输入联系电话" value="153 **** 3374" /></label></div>
    <div class="consent"><span>✓</span><p>我已阅读并同意《用户服务协议》和《隐私政策》，同意工作人员就本次需求与我联系。</p></div>
    <div class="simple-note"><b>仅需 3 项信息</b><p>本业务无需提交贸易增量相关的征信、网银或开票资料。</p></div>
    <button class="mobile-primary">提交需求　→</button>
  `)
}

function M06() {
  return phoneShell('我的', `
    <div class="profile-card"><span>刘</span><div><h2>刘立</h2><p>已绑定企业 · 示例科技有限公司</p></div><i>›</i></div>
    <div class="case-summary"><div><b>3</b><span>全部报单</span></div><div><b>1</b><span>处理中</span></div><div><b>1</b><span>待补资料</span></div><div><b>1</b><span>已完成</span></div></div>
    <div class="mobile-section"><div><h3>我的报单</h3><p>查看办理进度和补件要求</p></div><a>全部 ›</a></div>
    <div class="my-case"><div class="case-head"><span class="case-logo">↗</span><div><b>贸易增量服务</b><small>BD20260921001</small></div>${badge('待补资料')}</div><div class="timeline"><i class="done"></i><i class="done"></i><i></i><i></i></div><div class="timeline-label"><span>已提交</span><span>资料审核</span><span>业务办理</span><span>完成</span></div><p class="case-alert">请补充：税务局开票额度截图、网银限额及余额截图</p></div>
    <div class="my-case small-case"><div class="case-head"><span class="case-logo blue">◇</span><div><b>知识产权服务</b><small>BD20260918003</small></div>${badge('处理中')}</div></div>
    <div class="empty-line">— 暂无更多报单 —</div>
    <div class="profile-menu"><span>▣　企业资料</span><i>›</i><span>⌁　消息通知</span><i>›</i><span>◉　联系客服</span><i>›</i></div>
  `, 'mine')
}

function adminShell(active, title, content) {
  const nav = [['B01','⌂','工作台'],['B02','▤','报单管理'],['B03','◈','产品管理'],['B04','◎','客户资料'],['B06','⇩','导出中心']]
    .map(x => `<div class="admin-nav-item ${active === x[0] ? 'active' : ''}"><i>${x[1]}</i>${x[2]}${x[0] === 'B02' ? '<em>12</em>' : ''}</div>`).join('')
  return `<main class="admin-screen"><aside class="admin-sidebar"><div class="admin-logo"><span>WQ</span><div><b>万企商服通</b><small>BUSINESS SERVICE</small></div></div><div class="workspace"><span>刘</span><div><b>运营工作台</b><small>企业服务中心</small></div><i>⌄</i></div><nav>${nav}<hr/><label>系统设置</label><div class="admin-nav-item"><i>⚙</i>团队与权限</div></nav><div class="security-foot"><i></i>敏感资料已加密保护</div></aside><section class="admin-main"><header><span>管理后台　/　${title}</span><div><i>♢</i><span class="admin-avatar">刘</span><b>刘立</b><small>⌄</small></div></header><div class="admin-content">${content}</div></section></main>`
}

function pageTitle(kicker, title, desc, action = '') {
  return `<div class="admin-title"><div><small>${kicker}</small><h1>${title}</h1><p>${desc}</p></div>${action ? `<button class="admin-primary">${action}</button>` : ''}</div>`
}

function B01() {
  const rows = cases.slice(0,3).map(c => `<tr><td class="mono">${c[0]}</td><td><span class="company-mark">${c[1][0]}</span>${c[1]}</td><td><span class="service-tag">${c[2]}</span></td><td><div class="table-progress"><i style="width:${c[3].startsWith('11') || c[3].startsWith('3') ? 100 : 73}%"></i></div> ${c[3]}</td><td>${badge(c[4])}</td><td>${c[5]}</td><td>${c[6]}</td><td>···</td></tr>`).join('')
  return adminShell('B01','工作台', `${pageTitle('WEDNESDAY · 2026.09.23','早上好，刘立 👋','这里是今天的业务概览，一切都在有序推进。','查看全部报单　→')}
    <div class="metrics"><article class="metric featured"><span>待处理报单</span><b>12</b><small>较昨日　<em>+3</em></small></article><article class="metric"><span>本月已完成</span><b>28</b><small>完成率　<em class="green">87%</em></small></article><article class="metric"><span>待补资料</span><b>7</b><small>涉及客户　<em>5</em></small></article><article class="metric"><span>本月新增客户</span><b>16</b><small>累计客户　<em class="blue">128</em></small></article></div>
    <div class="dashboard-row"><article class="admin-panel chart-panel"><div class="panel-header"><div><h2>业务处理趋势</h2><p>近 7 天报单与完成数量</p></div><button>近 7 天⌄</button></div><div class="line-graph"><span>20</span><span>15</span><span>10</span><span>5</span><span>0</span><svg viewBox="0 0 650 230" preserveAspectRatio="none"><defs><linearGradient id="area" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="#2d7776" stop-opacity=".28"/><stop offset="1" stop-color="#2d7776" stop-opacity="0"/></linearGradient></defs><path d="M0 177 C70 162,86 130,140 146 S215 110,270 127 S340 91,397 103 S468 56,525 82 S597 45,650 60 L650 230 L0 230Z" fill="url(#area)"/><path d="M0 177 C70 162,86 130,140 146 S215 110,270 127 S340 91,397 103 S468 56,525 82 S597 45,650 60" fill="none" stroke="#2d7776" stroke-width="3"/><path d="M0 205 C70 199,91 183,140 190 S214 171,270 178 S343 159,397 171 S471 138,525 151 S596 125,650 134" fill="none" stroke="#e6ae50" stroke-width="2" stroke-dasharray="5 5"/></svg><div class="chart-dates"><i>09/17</i><i>09/18</i><i>09/19</i><i>09/20</i><i>09/21</i><i>09/22</i><i>09/23</i></div></div></article><article class="admin-panel focus"><div class="panel-header"><div><h2>需要关注</h2><p>优先处理这些事项</p></div><a>全部查看 ›</a></div><div><span class="focus-icon amber">!</span><p><b>示例科技有限公司</b><small>税务局开票额度截图待补充</small></p><em>2h前</em></div><div><span class="focus-icon rose">↗</span><p><b>衡阳星河贸易有限公司</b><small>资料已齐全，等待审核</small></p><em>4h前</em></div><div><span class="focus-icon sky">i</span><p><b>云智知识产权有限公司</b><small>客户提交了新的联系方式</small></p><em>昨天</em></div></article></div>
    <article class="admin-panel admin-table"><div class="panel-header"><div><h2>最近报单</h2><p>最新提交的业务单</p></div><a>进入报单管理 ›</a></div><table><thead><tr><th>报单编号</th><th>客户企业</th><th>服务项目</th><th>资料进度</th><th>状态</th><th>负责人</th><th>提交时间</th><th></th></tr></thead><tbody>${rows}</tbody></table></article>`)
}

function B02() {
  const rows = cases.map(c => `<tr><td class="mono">${c[0]}</td><td><span class="company-mark">${c[1][0]}</span><b>${c[1]}</b><small class="sub-row">湖南 · 衡阳</small></td><td><span class="service-tag">${c[2]}</span></td><td><div class="table-progress"><i style="width:${c[3].startsWith('8') ? 73 : 100}%"></i></div> ${c[3]}</td><td>${c[6]}</td><td>${badge(c[4])}</td><td>${c[5]}</td><td><a>查看详情</a></td></tr>`).join('')
  return adminShell('B02','报单管理', `${pageTitle('OPERATIONS','报单管理','查看、筛选和处理客户提交的业务报单。','＋ 新建报单')}
    <article class="admin-panel admin-table list-table"><div class="filters"><div class="search">⌕　搜索企业名称或报单编号</div><button>全部服务⌄</button><button>全部状态⌄</button><button>负责人⌄</button><button class="outline">重置</button><button class="outline teal">导出 Excel</button></div><table><thead><tr><th>报单编号</th><th>客户企业</th><th>服务项目</th><th>资料进度</th><th>提交时间</th><th>状态</th><th>负责人</th><th>操作</th></tr></thead><tbody>${rows}</tbody></table><div class="pagination"><span>共 12 条报单</span><button>‹</button><button class="active">1</button><button>2</button><button>›</button></div></article>
    <div class="state-strip"><span>${badge('待审核')} 新报单等待负责人审核</span><span>${badge('待补资料')} 客户需要补件</span><span>${badge('处理中')} 已进入业务办理</span><span>${badge('已完成')} 业务已归档</span></div>`)
}

function B03() {
  return adminShell('B03','产品管理', `${pageTitle('SERVICE CATALOG','产品与业务模块','后台维护服务内容、资料模板及上下架状态。','＋ 新增服务')}
    <div class="catalog-grid"><article class="catalog-card trade"><span>↗</span><div><small>TRADE INCREMENT</small><h2>贸易增量</h2><p>11 项资料模板 · 1 个服务</p></div><b>已启用</b></article><article class="catalog-card ip"><span>◇</span><div><small>INTELLECTUAL PROPERTY</small><h2>知识产权</h2><p>3 项基础字段 · 2 个服务</p></div><b>已启用</b></article><article class="catalog-card qualification"><span>✦</span><div><small>QUALIFICATION</small><h2>资质申报</h2><p>3 项基础字段 · 3 个服务</p></div><b>已启用</b></article></div>
    <article class="admin-panel admin-table"><div class="panel-header"><div><h2>服务项目</h2><p>调整排序、展示状态和资料要求</p></div><div class="small-tabs"><b>全部 6</b><span>已上架 5</span><span>已下架 1</span></div></div><table><thead><tr><th>服务名称</th><th>所属模块</th><th>资料模板</th><th>排序</th><th>展示状态</th><th>更新时间</th><th>操作</th></tr></thead><tbody><tr><td><b>贸易增量服务</b><small class="sub-row">月度企业材料提交</small></td><td>贸易增量</td><td>11 项资料</td><td>01</td><td>${badge('已上架')}</td><td>2026-09-22</td><td><a>编辑　下架</a></td></tr><tr><td><b>商标注册咨询</b><small class="sub-row">知识产权基础报单</small></td><td>知识产权</td><td>3 项信息</td><td>02</td><td>${badge('已上架')}</td><td>2026-09-20</td><td><a>编辑　下架</a></td></tr><tr><td><b>高新技术企业认定</b><small class="sub-row">科技资质申报</small></td><td>资质申报</td><td>3 项信息</td><td>03</td><td>${badge('已上架')}</td><td>2026-09-18</td><td><a>编辑　下架</a></td></tr><tr class="muted-row"><td><b>旧版企业咨询</b><small class="sub-row">保留历史报单引用</small></td><td>资质申报</td><td>3 项信息</td><td>99</td><td><span class="badge neutral">已下架</span></td><td>2026-09-11</td><td><a>编辑　上架</a></td></tr></tbody></table></article>
    <div class="empty-admin"><span>◇</span><b>暂无其他下架服务</b><p>新建服务后可在这里维护展示规则。</p></div>`)
}

function B04() {
  const rows = cases.map(c => `<tr><td class="mono">${c[0]}</td><td><span class="company-mark">${c[1][0]}</span><b>${c[1]}</b></td><td>${c[2]}</td><td><div class="table-progress"><i style="width:${c[3].startsWith('8') ? 73 : 100}%"></i></div> ${c[3]}</td><td>${badge(c[4])}</td><td>${c[6]}</td><td><a>审核　Excel　${c[2] === '贸易增量' ? 'ZIP' : ''}</a></td></tr>`).join('')
  return adminShell('B04','客户资料', `${pageTitle('CUSTOMER MATERIALS','客户资料与审核情况','查看企业资料状态并生成审核表或原始资料包。','↓ 下载用户导入模板')}
    <div class="customer-metrics"><article><span>企业客户</span><b>128</b><small>累计入库</small></article><article><span>资料已齐全</span><b>89</b><small class="green">完成率 70%</small></article><article><span>待补资料</span><b>23</b><small class="amber-text">需要跟进</small></article><article><span>本月已导出</span><b>36</b><small>Excel 24 · ZIP 12</small></article></div>
    <article class="admin-panel admin-table list-table"><div class="filters"><div class="search">⌕　公司名称 / 报单编号</div><button>业务类型⌄</button><button>资料状态⌄</button><button>提交月份⌄</button><button class="outline teal">导出审核表 Excel</button><button class="admin-primary mini">下载资料 ZIP</button></div><table><thead><tr><th>报单编号</th><th>公司名称</th><th>服务项目</th><th>资料完成度</th><th>审核状态</th><th>提交时间</th><th>操作</th></tr></thead><tbody>${rows}</tbody></table></article>
    <div class="export-notes"><article><span class="excel-icon">X</span><div><b>审核情况 Excel</b><p>企业基本信息、资料明细、审核结果和备注；不附带敏感原件。</p></div></article><article><span class="zip-icon">Z</span><div><b>原始资料 ZIP</b><p>仅贸易增量报单可生成，包含原始图片、PDF、视频及审核表。</p></div></article></div>`)
}

function B05() {
  const rows = materialItems.map((m,i) => `<tr><td><span class="material-index">${String(i+1).padStart(2,'0')}</span><b>${m[0]}</b></td><td>${i===5||i===6?'<span class="muted-text">未上传</span>':'<span class="file-pill">▣ 客户提交文件</span>'}</td><td>${badge(m[1])}</td><td>${i===4?'缺少公司名称及实时日期':i===5||i===6?'等待客户补件':'—'}</td><td><a>${i===5||i===6?'发送催补':'受控查看'}</a></td></tr>`).join('')
  return adminShell('B04','资料审核', `<div class="detail-top"><button>← 返回客户资料</button><div><button class="outline">导出审核表</button><button class="admin-primary mini">下载资料 ZIP</button></div></div>
    <div class="case-detail-head"><div><small>BD20260921001 · TRADE INCREMENT</small><h1>示例科技有限公司</h1><p>2026 年 09 月报单 · 提交于今天 10:18 · 负责人：刘立</p></div>${badge('待补资料')}</div>
    <div class="review-summary"><div><span>资料完成度</span><b>8 <small>/ 11</small></b></div><div class="wide-progress"><i style="width:73%"></i></div><div><span>审核完成</span><b>3 <small>/ 11</small></b></div><div><span>需补充</span><b class="amber-text">3 项</b></div></div>
    <div class="review-layout"><article class="admin-panel admin-table review-table"><div class="panel-header"><div><h2>11 项资料逐项审核</h2><p>敏感文件必须通过受控预览查看</p></div><span class="security-label">⌕ 权限校验已开启</span></div><table><thead><tr><th>资料明细</th><th>客户提交</th><th>审核结果</th><th>备注</th><th>操作</th></tr></thead><tbody>${rows}</tbody></table></article><aside><article class="admin-panel operation-card"><h2>处理操作</h2><label>整体状态<select><option>待补资料</option></select></label><label>客户可见说明<textarea>请补充税务局最新开票额度截图、网银限额及余额截图。</textarea></label><label>内部处理备注<textarea placeholder="仅后台人员可见"></textarea></label><button class="admin-primary full">保存审核结果</button></article><article class="admin-panel privacy-card"><span>⌕</span><div><b>敏感资料受控</b><p>身份证、征信、网银截图仅向有权限的审核人员开放；查看和下载均记录日志。</p></div></article></aside></div>`)
}

function B06() {
  return adminShell('B06','导出中心', `${pageTitle('EXPORT CENTER','客户资料导出','生成审核情况 Excel 与受控原始资料 ZIP。','＋ 新建导出任务')}
    <div class="export-preview"><article class="export-card teal"><span>▤</span><div><small>REVIEW SHEET</small><h2>审核表 Excel</h2><p>客户信息、资料状态、审核结果及备注</p></div><button>生成 Excel　→</button></article><article class="export-card gold"><span>⇩</span><div><small>MATERIAL ARCHIVE</small><h2>资料包 ZIP</h2><p>原始图片、PDF、视频及开票信息</p></div><button>生成 ZIP　→</button></article></div>
    <div class="export-layout"><article class="admin-panel package-tree"><div class="panel-header"><div><h2>导出预览</h2><p>示例科技有限公司 · BD20260921001</p></div>${badge('待生成')}</div><div class="tree-root"><b>示例科技有限公司_BD20260921001_资料包.zip</b><span>├─ 01_营业执照/</span><span>├─ 02_开票信息.xlsx</span><span>├─ 03_法人身份证/</span><span>├─ 04_无欠税证明/</span><span>├─ 05_开票额度截图/</span><span>├─ 06_网银限额截图/</span><span>├─ 07_网银余额截图/</span><span>├─ 08_法人征信/</span><span>├─ 09_企业征信/</span><span>├─ 10_办公室照片及视频/</span><span>├─ 11_水母报告链接.txt</span><span>└─ 客户资料审核情况.xlsx</span></div></article><article class="admin-panel export-rule"><h2>导出安全规则</h2><div><span>01</span><p><b>短时效链接</b><small>下载地址默认 30 分钟失效</small></p></div><div><span>02</span><p><b>权限控制</b><small>ZIP 仅敏感资料导出角色可生成</small></p></div><div><span>03</span><p><b>完整留痕</b><small>记录操作人、时间、报单号和下载次数</small></p></div><div><span>04</span><p><b>文件隔离</b><small>普通 Excel 不附带身份证、征信等原件</small></p></div></article></div>
    <article class="admin-panel admin-table export-history"><div class="panel-header"><div><h2>导出记录</h2><p>最近生成的文件与下载情况</p></div><span class="security-label">⌕ 已启用操作留痕</span></div><table><thead><tr><th>文件名称</th><th>类型</th><th>创建时间</th><th>操作人</th><th>下载次数</th><th>状态</th><th>操作</th></tr></thead><tbody><tr><td class="mono">示例科技有限公司_BD20260921001_资料包.zip</td><td><span class="file-type zip">ZIP</span></td><td>今天 10:32</td><td>刘立</td><td>2 次</td><td>${badge('可下载')}</td><td><a>下载</a></td></tr><tr><td class="mono">2026-09-客户审核情况.xlsx</td><td><span class="file-type excel">XLSX</span></td><td>昨天 17:02</td><td>李敏</td><td>4 次</td><td>${badge('可下载')}</td><td><a>下载</a></td></tr></tbody></table></article>`)
}

const renderers = { M01, M02, M03, M04, M05, M06, B01, B02, B03, B04, B05, B06 }
document.title = `${screen} · 万企商服通高保真预览`
document.body.dataset.screen = screen
document.querySelector('#app').innerHTML = (renderers[screen] || M01)()
