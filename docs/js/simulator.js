/**
 * PensionAlchemy Interactive App Simulator (simulator.js)
 * 실제 안드로이드 앱과 100% 동일한 화면, 반응형 인터랙션, 실시간 시뮬레이션 엔진 연동
 */

const AppSimulator = {
    // 현재 앱 상태
    state: {
        currentTab: 'DASHBOARD', // DASHBOARD, ASSETS, PENSIONS, CALCULATOR, SETTINGS, GUIDE
        selectedAge: 60,
        assetTab: 0, // 0: 자산/부채, 1: 정기소득
        pensionClaimOffset: 0, // 국민연금 조기/연기 (-5 ~ +5)
        calculatorMode: 0, // 0: 적립·예탁, 1: 자산 인출, 2: 고갈 타이머, 3: 목표 필요자산, 4: 국민연금 손익, 5: 절세 연금술사
        settingsTab: 0, // 0: 기본 프로필, 1: 세법·정책 변수
        guideCategory: 0, // 0: 앱 사용 도움말, 1: 재무비법, 2: 정부 포털, 3: 유튜브 채널

        // 데이터 모델
        profile: Presets.preset40s().profile,
        pensions: Presets.preset40s().pensions,
        assets: Presets.preset40s().assets,
        incomes: Presets.preset40s().incomes,

        // 계산기 파라미터
        calc: {
            accInitialDeposit: 50000000,
            accMonthlyDeposit: 1000000,
            accPeriodYears: 20,
            accInterestRate: 6.0,
            accDepositType: 0, // 0: 동시, 1: 적립식만, 2: 거치식만

            withStartingWealth: 500000000,
            withAnnualGrowthRate: 5.0,
            withIsFixedAmount: true,
            withMonthlyWithdrawal: 2500000,
            withAnnualWithdrawalRate: 4.0,

            depCurrentWealth: 400000000,
            depMonthlyWithdrawal: 2500000,
            depAnnualGrowthRate: 5.0,
            depInflationRate: 2.0,

            reqDesiredMonthlyExpense: 3000000,
            reqRetirementYears: 30,
            reqExpectedReturnRate: 4.5,
            reqCurrentAge: 40,
            reqTargetRetirementAge: 60,

            beNormalMonthlyAmount: 1600000,
            beStatutoryAge: 65,

            taxAnnualContribution: 9000000,
            taxIsLowIncomeTier: true
        },

        // 시뮬레이션 요약 결과 캐시
        summary: null
    },

    init() {
        this.runSimulation();
        this.renderAll();
        this.setupEventListeners();
    },

    runSimulation() {
        try {
            this.state.summary = SimulationEngine.runComprehensiveSimulation(
                this.state.profile,
                this.state.pensions,
                this.state.assets,
                this.state.incomes
            );
        } catch (err) {
            console.error('Simulation execution error:', err);
            // 비정상 상태 감지 시 기본 40대 프리셋으로 안전 복원 후 재시도
            try {
                const fallback = Presets.preset40s();
                this.state.profile = fallback.profile;
                this.state.pensions = fallback.pensions;
                this.state.assets = fallback.assets;
                this.state.incomes = fallback.incomes;
                this.state.summary = SimulationEngine.runComprehensiveSimulation(
                    this.state.profile,
                    this.state.pensions,
                    this.state.assets,
                    this.state.incomes
                );
            } catch (retryErr) {
                console.error('Fallback simulation failed:', retryErr);
            }
        }

        // 초기 selectedAge 조정
        if (this.state.summary && (!this.state.selectedAge || this.state.selectedAge < this.state.summary.currentAge)) {
            this.state.selectedAge = this.state.summary.currentAge < this.state.summary.retirementAge
                ? this.state.summary.retirementAge
                : this.state.summary.currentAge;
        }
    },

    loadPreset(presetName) {
        if (!Presets[presetName]) return;
        const data = Presets[presetName]();
        this.state.profile = data.profile;
        this.state.pensions = data.pensions;
        this.state.assets = data.assets;
        this.state.incomes = data.incomes;
        this.state.selectedAge = data.profile.retirementAge;

        // 국민연금 offset 반영
        const nat = this.state.pensions.find(p => p.type === 'NATIONAL');
        this.state.pensionClaimOffset = nat ? (nat.claimOffsetYears || 0) : 0;

        this.runSimulation();
        this.renderAll();
    },

    switchTab(tabName) {
        this.state.currentTab = tabName;
        this.renderAppScreen();
        this.updateNavButtons();
    },

    setAge(age) {
        this.state.selectedAge = Number(age);
        this.renderDashboardInspector();
        this.drawTrajectoryChart();
    },

    setPensionOffset(offset) {
        this.state.pensionClaimOffset = Number(offset);
        const nat = this.state.pensions.find(p => p.type === 'NATIONAL');
        if (nat) {
            nat.claimOffsetYears = Number(offset);
        }
        this.runSimulation();
        this.renderAll();
    },

    setCalculatorMode(modeIndex) {
        this.state.calculatorMode = Number(modeIndex);
        this.renderCalculatorScreen();
    },

    renderAll() {
        this.renderAppScreen();
        this.updateNavButtons();
        // 동기화된 외부 매뉴얼 패널이 있다면 갱신
        if (window.ManualController) {
            window.ManualController.onSimulatorUpdated(this.state);
        }
    },

    updateNavButtons() {
        document.querySelectorAll('.app-nav-item').forEach(btn => {
            const tab = btn.dataset.tab;
            if (tab === this.state.currentTab) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
    },

    renderAppScreen() {
        const container = document.getElementById('phone-screen-content');
        if (!container) return;

        const headerTitle = document.getElementById('phone-header-title');
        const headerSubtitle = document.getElementById('phone-header-subtitle');

        const titles = {
            DASHBOARD: { title: '연금술사', sub: '대시보드 종합 진단' },
            ASSETS: { title: '자산관리', sub: '보유 자산 및 정기 소득' },
            PENSIONS: { title: '연금플랜', sub: '3층 연금 피라미드' },
            CALCULATOR: { title: '재무 계산기', sub: '6대 전문 금융 공식' },
            SETTINGS: { title: '환경 설정', sub: '생애주기 및 정책 변수' },
            GUIDE: { title: '연금술사 가이드', sub: '재무비법 & 공식포털' }
        };

        const t = titles[this.state.currentTab] || titles.DASHBOARD;
        if (headerTitle) headerTitle.textContent = t.title;
        if (headerSubtitle) headerSubtitle.textContent = t.sub;

        switch (this.state.currentTab) {
            case 'DASHBOARD':
                container.innerHTML = this.getDashboardHTML();
                this.initDashboardCharts();
                break;
            case 'ASSETS':
                container.innerHTML = this.getAssetsHTML();
                break;
            case 'PENSIONS':
                container.innerHTML = this.getPensionsHTML();
                break;
            case 'CALCULATOR':
                container.innerHTML = this.getCalculatorHTML();
                break;
            case 'SETTINGS':
                container.innerHTML = this.getSettingsHTML();
                break;
            case 'GUIDE':
                container.innerHTML = this.getGuideHTML();
                break;
        }
    },

    // ──────────────────────────────────────────────
    // 1. 대시보드 화면 HTML 생성
    // ──────────────────────────────────────────────
    getDashboardHTML() {
        if (!this.state.summary) {
            this.runSimulation();
        }
        const s = this.state.summary;
        if (!s) {
            return `
                <div class="screen-scroll-container" style="padding: 24px; text-align: center;">
                    <div class="app-card">
                        <h3>시뮬레이션 데이터 준비 중</h3>
                        <p style="color: var(--text-secondary); margin: 12px 0;">데이터를 초기화하는 중입니다.</p>
                        <button class="action-btn btn-primary" onclick="AppSimulator.loadPreset('preset40s')">40대 기본 프리셋 불러오기</button>
                    </div>
                </div>
            `;
        }
        const hs = s.healthScore || { score: 0, grade: '산출중', gradeColorHex: '#3B82F6', feedbackMessage: '' };
        const civ = s.crevasseInfo || { hasCrevasse: false };

        return `
            <div class="screen-scroll-container">
                <!-- 1. 메인 순자산 카드 -->
                <div class="app-card primary-gradient-card">
                    <div class="card-row-between">
                        <span class="card-caption">현재 순자산 (Net Worth)</span>
                        <span class="card-badge-sub">기준 나이: ${s.currentAge}세</span>
                    </div>
                    <div class="net-worth-amount">${CurrencyFormatter.formatKoreanWon(s.currentNetWorth)}</div>
                    <div class="card-row-between sub-assets-row">
                        <span>총자산 ${CurrencyFormatter.formatKoreanWon(s.currentTotalAssets, true)}</span>
                        <span>총부채 ${CurrencyFormatter.formatKoreanWon(s.currentTotalDebt, true)}</span>
                    </div>
                </div>

                <!-- 2. 은퇴 준비 건강도 점수 배너 -->
                <div class="app-card score-banner-card">
                    <div class="score-badge" style="background-color: ${hs.gradeColorHex}22; border: 2px solid ${hs.gradeColorHex};">
                        <span class="score-number" style="color: ${hs.gradeColorHex};">${hs.score}</span>
                        <span class="score-grade" style="color: ${hs.gradeColorHex};">${hs.grade.split(' ')[0]}</span>
                    </div>
                    <div class="score-text-col">
                        <div class="score-title" style="color: ${hs.gradeColorHex};">은퇴 준비 건강도: ${hs.grade}</div>
                        <div class="score-feedback">${hs.feedbackMessage}</div>
                    </div>
                </div>

                <!-- 3. 소득 크레바스 경고 배너 -->
                ${civ.hasCrevasse ? `
                <div class="app-card warning-banner-card">
                    <div class="warning-icon">⚠️</div>
                    <div class="warning-text-col">
                        <div class="warning-title">소득 크레바스(공백기) 주의: ${civ.startAge}세 ~ ${civ.endAge}세</div>
                        <div class="warning-desc">은퇴 후 국민연금 개시까지 ${civ.durationYears}년간 월 ${CurrencyFormatter.formatKoreanWon(civ.monthlyShortfall, true)} 부족 (필요 브릿지자금: ${CurrencyFormatter.formatKoreanWon(civ.totalRequiredBridgeFund, true)})</div>
                    </div>
                </div>
                ` : ''}

                <!-- 4. 2x2 핵심 지표 카드 -->
                <div class="metrics-grid">
                    <div class="metric-card">
                        <div class="metric-header">
                            <span class="metric-icon">📈</span>
                            <span class="metric-name">소득 대체율</span>
                        </div>
                        <div class="metric-value text-emerald">${CurrencyFormatter.formatPercent(s.incomeReplacementRate)}</div>
                        <div class="metric-sub">권장치: 50~60%</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-header">
                            <span class="metric-icon">🛡️</span>
                            <span class="metric-name">자산 고갈 시점</span>
                        </div>
                        <div class="metric-value ${s.isSafeRetirement ? 'text-emerald' : 'text-danger'}">
                            ${s.isSafeRetirement ? '100세+ 안전' : s.depletionAge + '세 고갈'}
                        </div>
                        <div class="metric-sub">${s.isSafeRetirement ? '평생 자산 유지' : '지출 조정 필요'}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-header">
                            <span class="metric-icon">🏛️</span>
                            <span class="metric-name">65세 월연금</span>
                        </div>
                        <div class="metric-value text-cyan">${CurrencyFormatter.formatKoreanWon(s.postRetirementMonthlyPension, true)}/월</div>
                        <div class="metric-sub">국민+퇴직+개인 합산</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-header">
                            <span class="metric-icon">⭐</span>
                            <span class="metric-name">피크 자산</span>
                        </div>
                        <div class="metric-value text-amber">${CurrencyFormatter.formatKoreanWon(s.peakAssetValue, true)}</div>
                        <div class="metric-sub">${s.peakAssetAge}세 도달 예상</div>
                    </div>
                </div>

                <!-- 5. 생애 자산 궤적 차트 -->
                <div class="app-card chart-card">
                    <div class="chart-header">
                        <span class="chart-title">생애 자산 궤적 (${s.currentAge}세 ~ ${s.endAge}세)</span>
                        <span class="chart-legend-badge">실시간 시뮬레이션</span>
                    </div>
                    <div class="canvas-wrapper">
                        <canvas id="canvas-trajectory" height="220"></canvas>
                    </div>
                    <div class="chart-legend-row">
                        <span class="legend-item"><span class="legend-dot" style="background:#10B981;"></span>순자산</span>
                        <span class="legend-item"><span class="legend-dot" style="background:#3B82F6;"></span>총자산</span>
                        <span class="legend-item"><span class="legend-dot" style="background:#F43F5E;"></span>부채</span>
                        <span class="legend-item"><span class="legend-line-dashed"></span>은퇴(${s.retirementAge}세)</span>
                    </div>
                </div>

                <!-- 6. 나이별 정밀 인스펙터 슬라이더 & 분해 카드 -->
                <div class="app-card inspector-card" id="dashboard-inspector-box">
                    ${this.getInspectorCardInnerHTML()}
                </div>

                <!-- 7. 3층 연금 스택 바 차트 -->
                <div class="app-card chart-card">
                    <div class="chart-header">
                        <span class="chart-title">연령대별 3층 연금 현금흐름 스택</span>
                        <span class="chart-legend-badge">월수령액 기준</span>
                    </div>
                    <div class="canvas-wrapper">
                        <canvas id="canvas-cashflow-stack" height="190"></canvas>
                    </div>
                    <div class="chart-legend-row">
                        <span class="legend-item"><span class="legend-dot" style="background:#3B82F6;"></span>1층 국민</span>
                        <span class="legend-item"><span class="legend-dot" style="background:#8B5CF6;"></span>2층 퇴직</span>
                        <span class="legend-item"><span class="legend-dot" style="background:#10B981;"></span>3층 개인</span>
                        <span class="legend-item"><span class="legend-dot" style="background:#F59E0B;"></span>주택연금</span>
                    </div>
                </div>

                <!-- 8. 포트폴리오 자산 배분 도넛 차트 -->
                <div class="app-card chart-card">
                    <div class="chart-header">
                        <span class="chart-title">현재 포트폴리오 자산 배분</span>
                    </div>
                    <div class="donut-chart-container">
                        <canvas id="canvas-donut" width="160" height="160"></canvas>
                        <div class="donut-legend-list">
                            <div class="donut-legend-item">
                                <span class="legend-dot" style="background:#3B82F6;"></span>
                                <span class="donut-legend-name">금융자산</span>
                                <span class="donut-legend-val">${CurrencyFormatter.formatKoreanWon(s.currentFinancialAssets, true)}</span>
                            </div>
                            <div class="donut-legend-item">
                                <span class="legend-dot" style="background:#10B981;"></span>
                                <span class="donut-legend-name">연금자산</span>
                                <span class="donut-legend-val">${CurrencyFormatter.formatKoreanWon(s.currentPensionAssets, true)}</span>
                            </div>
                            <div class="donut-legend-item">
                                <span class="legend-dot" style="background:#F59E0B;"></span>
                                <span class="donut-legend-name">부동산</span>
                                <span class="donut-legend-val">${CurrencyFormatter.formatKoreanWon(s.currentRealEstateAssets, true)}</span>
                            </div>
                            <div class="donut-legend-item">
                                <span class="legend-dot" style="background:#F43F5E;"></span>
                                <span class="donut-legend-name">부채</span>
                                <span class="donut-legend-val">-${CurrencyFormatter.formatKoreanWon(s.currentTotalDebt, true)}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 9. 대한민국 순자산 백분위 분포 차트 -->
                <div class="app-card chart-card">
                    <div class="chart-header">
                        <span class="chart-title">대한민국 순자산 백분위 분포</span>
                        <span class="chart-legend-badge">2025 가계금융복지조사</span>
                    </div>
                    <div class="canvas-wrapper">
                        <canvas id="canvas-distribution" height="180"></canvas>
                    </div>
                    <div class="distribution-info-bar">
                        ${this.getDistributionInfoText()}
                    </div>
                </div>

                <div style="height: 60px;"></div>
            </div>
        `;
    },

    getInspectorCardInnerHTML() {
        const s = this.state.summary;
        const curAge = this.state.selectedAge;
        const item = s.yearlyResults.find(r => r.age === curAge) || s.yearlyResults[0];

        return `
            <div class="card-row-between">
                <span class="inspector-title">나이별 정밀 인스펙터</span>
                <span class="inspector-badge">${item.age}세 (${item.stage})</span>
            </div>

            <div class="slider-container">
                <input type="range" class="app-slider" id="inspector-age-slider"
                       min="${s.currentAge}" max="${s.endAge}" value="${curAge}"
                       oninput="AppSimulator.setAge(this.value)">
                <div class="slider-ticks">
                    <span>${s.currentAge}세</span>
                    <span>은퇴(${s.retirementAge}세)</span>
                    <span>80세</span>
                    <span>${s.endAge}세</span>
                </div>
            </div>

            <!-- 1) 생애 순자산 -->
            <div class="inspector-row-box highlight-box">
                <div class="box-left">
                    <span class="box-icon">💼</span>
                    <div>
                        <div class="box-label">예상 순자산</div>
                        <div class="box-sub">총자산 - 총부채</div>
                    </div>
                </div>
                <div class="box-amount">${CurrencyFormatter.formatKoreanWon(item.netAssetValue)}</div>
            </div>

            <!-- 2) 월간 현금흐름 밸런스 -->
            <div class="inspector-row-box">
                <div class="cashflow-list">
                    <div class="cf-row">
                        <span><span class="cf-dot text-emerald">▲</span> 월 총소득</span>
                        <span class="cf-val text-emerald">+${CurrencyFormatter.formatKoreanWon(item.monthlyTotalIncome)}/월</span>
                    </div>
                    <div class="cf-row">
                        <span><span class="cf-dot text-danger">▼</span> 월 생활지출</span>
                        <span class="cf-val text-danger">-${CurrencyFormatter.formatKoreanWon(item.monthlyExpenses)}/월</span>
                    </div>
                    <div class="cf-divider"></div>
                    <div class="cf-row font-bold">
                        <span>
                            ${item.monthlyNetCashFlow >= 0 ? '💰 월 잉여금(흑자)' : '🚨 월 적자(부족금)'}
                        </span>
                        <span class="cf-val ${item.monthlyNetCashFlow >= 0 ? 'text-emerald' : 'text-danger'}">
                            ${item.monthlyNetCashFlow >= 0 ? '+' : ''}${CurrencyFormatter.formatKoreanWon(item.monthlyNetCashFlow)}/월
                        </span>
                    </div>
                </div>
            </div>

            <!-- 3) 연금 실수령 및 과세 내역 -->
            <div class="inspector-row-box">
                <div class="cashflow-list">
                    <div class="cf-row font-bold">
                        <span><span class="cf-dot text-emerald">🛡️</span> 연금 실수령액(세후)</span>
                        <span class="cf-val text-emerald">${CurrencyFormatter.formatKoreanWon(item.monthlyPensionIncome)}/월</span>
                    </div>
                    ${item.monthlyPensionTax > 0 ? `
                    <div class="cf-row sub-row text-muted">
                        <span>• 연금 세전 총액</span>
                        <span>${CurrencyFormatter.formatKoreanWon(item.grossMonthlyPensionIncome)}/월</span>
                    </div>
                    <div class="cf-row sub-row text-amber">
                        <span>• 예상 원천징수 세금</span>
                        <span>-${CurrencyFormatter.formatKoreanWon(item.monthlyPensionTax)}/월</span>
                    </div>
                    ` : ''}
                </div>
            </div>

            <!-- 4) 세법 및 정책 경고 -->
            ${item.isHealthInsuranceDisqualified ? `
            <div class="policy-alert-banner alert-amber">
                ⚠️ 공적연금 연 2,000만원 초과: 건강보험 피부양자 탈락 및 지역가입자 전환 주의 구간입니다.
            </div>
            ` : ''}
            ${item.isPrivatePensionLimitExceeded ? `
            <div class="policy-alert-banner alert-cyan">
                ℹ️ 사적연금 연 1,500만원 초과: 초과분에 16.5% 분리과세가 적용됩니다. 수령 기간 연장을 고려하세요.
            </div>
            ` : ''}
        `;
    },

    renderDashboardInspector() {
        const box = document.getElementById('dashboard-inspector-box');
        if (box) {
            box.innerHTML = this.getInspectorCardInnerHTML();
        }
    },

    getDistributionInfoText() {
        const curEok = this.state.summary.currentNetWorth / 100000000.0;
        const topPct = LogNormalDistribution.topPercent(
            curEok,
            this.state.profile.policySettings.wealthDistributionMean,
            this.state.profile.policySettings.wealthDistributionStdDev
        );
        return `
            <span>내 순자산: <strong>${CurrencyFormatter.formatKoreanWon(this.state.summary.currentNetWorth)}</strong></span>
            <span>대한민국 상위 <strong>${topPct.toFixed(1)}%</strong></span>
        `;
    },

    // ──────────────────────────────────────────────
    // ──────────────────────────────────────────────
    // 2. 자산관리 화면 HTML
    // ──────────────────────────────────────────────
    getAssetsHTML() {
        const isDebt = (a) => Boolean(a.isLiability || a.type === 'DEBT');
        const totalAssets = this.state.assets.filter(a => !isDebt(a)).reduce((s, a) => s + Number(a.currentValue || 0), 0);
        const totalDebt = this.state.assets.filter(a => isDebt(a)).reduce((s, a) => s + Number(a.currentValue || 0), 0);
        const netWorth = Math.max(0, totalAssets - totalDebt);
        const totalMonthlyIncome = this.state.incomes.reduce((s, i) => s + Number(i.monthlyAmount || 0), 0);

        return `
            <div class="screen-scroll-container">
                <!-- 상단 탭 전환 -->
                <div class="app-tab-switch">
                    <button class="tab-btn ${this.state.assetTab === 0 ? 'active' : ''}" onclick="AppSimulator.setAssetTab(0)">
                        자산 및 부채 (${this.state.assets.length})
                    </button>
                    <button class="tab-btn ${this.state.assetTab === 1 ? 'active' : ''}" onclick="AppSimulator.setAssetTab(1)">
                        정기 소득 (${this.state.incomes.length})
                    </button>
                </div>

                <!-- 자산 종합 요약 헤더 -->
                <div class="app-card sub-summary-card">
                    <div class="sub-summary-row">
                        <div>
                            <div class="sub-summary-label">순자산 (자산 - 부채)</div>
                            <div class="sub-summary-val text-emerald">${CurrencyFormatter.formatKoreanWon(netWorth)}</div>
                        </div>
                        <div>
                            <div class="sub-summary-label">정기 월소득 합계</div>
                            <div class="sub-summary-val text-cyan">${CurrencyFormatter.formatKoreanWon(totalMonthlyIncome, true)}/월</div>
                        </div>
                    </div>
                    <div class="sub-summary-sub">
                        <span>총자산: ${CurrencyFormatter.formatKoreanWon(totalAssets, true)}</span>
                        <span>총부채: ${CurrencyFormatter.formatKoreanWon(totalDebt, true)}</span>
                    </div>
                </div>

                ${this.state.assetTab === 0 ? `
                <!-- 자산 목록 -->
                <div class="item-list-header">
                    <span>보유 자산 및 대출 부채 목록</span>
                    <button class="mini-add-btn" onclick="AppSimulator.openAddAssetModal()">+ 자산 추가</button>
                </div>
                <div class="items-list">
                    ${this.state.assets.map(a => {
                        const itemIsDebt = isDebt(a);
                        return `
                        <div class="app-card item-card ${itemIsDebt ? 'debt-item-card' : ''}">
                            <div class="item-card-header">
                                <div class="item-title-row">
                                    <span class="item-type-badge ${itemIsDebt ? 'badge-danger' : 'badge-emerald'}">
                                        ${AssetType[a.type]?.displayName || a.type}
                                    </span>
                                    <span class="item-name">${a.name}</span>
                                </div>
                                <div style="display:flex; align-items:center; gap:8px;">
                                    <span class="item-val ${itemIsDebt ? 'text-danger' : 'text-primary'}">
                                        ${itemIsDebt ? '-' : ''}${CurrencyFormatter.formatKoreanWon(a.currentValue)}
                                    </span>
                                    <button class="item-delete-btn" onclick="AppSimulator.deleteAsset('${a.id}')" title="삭제">✕</button>
                                </div>
                            </div>
                            <div class="item-card-body">
                                <span>${itemIsDebt ? '대출금리' : '기대수익률'}: <strong>${a.expectedGrowthRate}%</strong></span>
                                ${itemIsDebt ? `
                                <span>상환방식: <strong>${RepaymentMethod[a.repaymentMethod]?.displayName || a.repaymentMethod}</strong> (${a.maturityYears || 10}년)</span>
                                ` : ''}
                            </div>
                        </div>
                        `;
                    }).join('')}
                </div>
                ` : `
                <!-- 소득 목록 -->
                <div class="item-list-header">
                    <span>은퇴 전 정기 근로 및 기타 소득</span>
                    <button class="mini-add-btn" onclick="AppSimulator.openAddIncomeModal()">+ 소득 추가</button>
                </div>
                <div class="items-list">
                    ${this.state.incomes.map(i => `
                        <div class="app-card item-card">
                            <div class="item-card-header">
                                <div class="item-title-row">
                                    <span class="item-type-badge badge-blue">
                                        ${IncomeType[i.type]?.displayName || i.type}
                                    </span>
                                    <span class="item-name">${i.name}</span>
                                </div>
                                <div style="display:flex; align-items:center; gap:8px;">
                                    <span class="item-val text-emerald">
                                        +${CurrencyFormatter.formatKoreanWon(i.monthlyAmount)}/월
                                    </span>
                                    <button class="item-delete-btn" onclick="AppSimulator.deleteIncome('${i.id}')" title="삭제">✕</button>
                                </div>
                            </div>
                            <div class="item-card-body">
                                <span>종료 나이: <strong>${i.endAge}세 은퇴</strong></span>
                                <span>연간 소득상승률: <strong>${i.expectedGrowthRate}%</strong></span>
                            </div>
                        </div>
                    `).join('')}
                </div>
                `}

                <div style="height: 60px;"></div>
            </div>
        `;
    },

    setAssetTab(tabIndex) {
        this.state.assetTab = tabIndex;
        this.renderAppScreen();
    },

    // ──────────────────────────────────────────────
    // 3. 연금플랜 화면 HTML
    // ──────────────────────────────────────────────
    getPensionsHTML() {
        const pensions = this.state.pensions;
        const national = pensions.find(p => p.type === 'NATIONAL');
        const totalMonthly = pensions.reduce((s, p) => s + Number(p.expectedMonthlyAmount), 0);
        const totalBalance = pensions.reduce((s, p) => s + Number(p.currentBalance || 0), 0);

        const offset = this.state.pensionClaimOffset;
        const ratePercent = offset < 0 ? offset * 6.0 : offset * 7.2;
        const adjustedNational = national 
            ? Math.round(Number(national.expectedMonthlyAmount) * (1.0 + ratePercent / 100.0))
            : 0;

        return `
            <div class="screen-scroll-container">
                <!-- 1. 3층 연금 피라미드 요약 카드 -->
                <div class="app-card pyramid-summary-card">
                    <div class="pyramid-title">대한민국 3층 연금 안전망 체계</div>
                    
                    <div class="tier-bars-stack">
                        <div class="tier-bar tier-3-personal">
                            <div class="tier-label"><span>3층 세제적격</span> 개인연금저축</div>
                            <div class="tier-val">${CurrencyFormatter.formatKoreanWon(pensions.filter(p => p.type === 'PERSONAL').reduce((s, p) => s + Number(p.expectedMonthlyAmount), 0), true)}/월</div>
                        </div>
                        <div class="tier-bar tier-3-annuity">
                            <div class="tier-label"><span>3층 비과세</span> 개인연금보험</div>
                            <div class="tier-val">${CurrencyFormatter.formatKoreanWon(pensions.filter(p => p.type === 'ANNUITY_INSURANCE').reduce((s, p) => s + Number(p.expectedMonthlyAmount), 0), true)}/월</div>
                        </div>
                        <div class="tier-bar tier-2-retire">
                            <div class="tier-label"><span>2층 표준보장</span> 퇴직연금/IRP</div>
                            <div class="tier-val">${CurrencyFormatter.formatKoreanWon(pensions.filter(p => p.type === 'RETIREMENT').reduce((s, p) => s + Number(p.expectedMonthlyAmount), 0), true)}/월</div>
                        </div>
                        <div class="tier-bar tier-1-national">
                            <div class="tier-label"><span>1층 기본보장</span> 국민연금</div>
                            <div class="tier-val">${CurrencyFormatter.formatKoreanWon(pensions.filter(p => p.type === 'NATIONAL').reduce((s, p) => s + Number(p.expectedMonthlyAmount), 0), true)}/월</div>
                        </div>
                    </div>

                    <div class="card-row-between pyramid-footer">
                        <span>적립 잔액: ${CurrencyFormatter.formatKoreanWon(totalBalance, true)}</span>
                        <span>예상 합산: <strong class="text-emerald">${CurrencyFormatter.formatKoreanWon(totalMonthly, true)}/월</strong></span>
                    </div>
                </div>

                <!-- 2. 국민연금 조기/연기 수령 특화 제어 카드 -->
                ${national ? `
                <div class="app-card national-slider-card">
                    <div class="card-row-between">
                        <span class="inspector-title">국민연금 조기/연기 수령 옵션</span>
                        <span class="badge ${offset === 0 ? 'badge-blue' : offset > 0 ? 'badge-emerald' : 'badge-amber'}">
                            ${offset < 0 ? `조기 ${-offset}년 (${ratePercent.toFixed(1)}%)` : offset > 0 ? `연기 ${offset}년 (+${ratePercent.toFixed(1)}%)` : '정상 수령 (100%)'}
                        </span>
                    </div>

                    <div class="slider-container" style="margin-top:12px;">
                        <input type="range" class="app-slider slider-national" id="national-claim-slider"
                               min="-5" max="5" step="1" value="${offset}"
                               oninput="AppSimulator.setPensionOffset(this.value)">
                        <div class="slider-ticks">
                            <span>조기(60세, -30%)</span>
                            <span>정상(${national.startAge}세)</span>
                            <span>연기(70세, +36%)</span>
                        </div>
                    </div>

                    <div class="national-result-banner">
                        <div>개시 나이: <strong>${national.startAge + offset}세</strong></div>
                        <div>수령 예상액: <strong class="text-emerald">${CurrencyFormatter.formatKoreanWon(adjustedNational)}/월</strong></div>
                    </div>
                </div>
                ` : ''}

                <!-- 3. 연금 리스트 헤더 -->
                <div class="item-list-header">
                    <span>나의 연금 플랜 목록 (${pensions.length})</span>
                    <button class="mini-add-btn" onclick="AppSimulator.openAddPensionModal()">+ 연금 추가</button>
                </div>

                <!-- 4. 연금 목록 카드 -->
                <div class="items-list">
                    ${pensions.map(p => `
                        <div class="app-card item-card">
                            <div class="item-card-header">
                                <div class="item-title-row">
                                    <span class="item-type-badge" style="background-color: ${PensionType[p.type]?.color}22; color: ${PensionType[p.type]?.color};">
                                        ${PensionType[p.type]?.displayName || p.type}
                                    </span>
                                    <span class="item-name">${p.name}</span>
                                </div>
                                <div style="display:flex; align-items:center; gap:8px;">
                                    <span class="item-val text-emerald">
                                        ${CurrencyFormatter.formatKoreanWon(p.expectedMonthlyAmount)}/월
                                    </span>
                                    ${p.type !== 'NATIONAL' ? `
                                    <button class="item-delete-btn" onclick="AppSimulator.deletePension('${p.id}')" title="삭제">✕</button>
                                    ` : ''}
                                </div>
                            </div>
                            <div class="item-card-body">
                                <span>수령기간: <strong>${p.startAge}세 ~ ${p.endAge}세</strong></span>
                                <span>운용수익률: <strong>${p.expectedGrowthRate || 0}%</strong></span>
                            </div>
                            ${p.currentBalance > 0 || p.monthlyContribution > 0 ? `
                            <div class="item-card-footer">
                                <span>현재적립: ${CurrencyFormatter.formatKoreanWon(p.currentBalance, true)}</span>
                                <span>월납입: ${CurrencyFormatter.formatKoreanWon(p.monthlyContribution, true)} (종료:${p.contributionEndAge}세)</span>
                            </div>
                            ` : ''}
                        </div>
                    `).join('')}
                </div>

                <div style="height: 60px;"></div>
            </div>
        `;
    },

    // ──────────────────────────────────────────────
    // 4. 6대 계산기 화면 HTML
    // ──────────────────────────────────────────────
    getCalculatorHTML() {
        const modes = ["적립·예탁", "자산 인출", "고갈 타이머", "목표 필요자산", "국민연금 손익", "절세 연금술사"];

        return `
            <div class="screen-scroll-container">
                <!-- 6대 모드 탭 바 -->
                <div class="calc-modes-scroll-bar">
                    ${modes.map((name, idx) => `
                        <button class="calc-mode-btn ${this.state.calculatorMode === idx ? 'active' : ''}"
                                onclick="AppSimulator.setCalculatorMode(${idx})">
                            ${name}
                        </button>
                    `).join('')}
                </div>

                <!-- 선택된 계산기 화면 뷰 -->
                <div class="calc-active-view">
                    ${this.getActiveCalculatorViewHTML()}
                </div>

                <div style="height: 60px;"></div>
            </div>
        `;
    },

    getActiveCalculatorViewHTML() {
        switch (this.state.calculatorMode) {
            case 0: return this.getCalcAccumulationHTML();
            case 1: return this.getCalcWithdrawalHTML();
            case 2: return this.getCalcDepletionHTML();
            case 3: return this.getCalcRequiredWealthHTML();
            case 4: return this.getCalcBreakEvenHTML();
            case 5: return this.getCalcTaxBenefitHTML();
            default: return '';
        }
    },

    // 계산기 모드 1: 적립·예탁 미래가치
    getCalcAccumulationHTML() {
        const c = this.state.calc;
        const res = CalculatorsEngine.calculateAccumulation(
            c.accDepositType === 2 ? 0 : c.accMonthlyDeposit,
            c.accPeriodYears,
            c.accInterestRate,
            c.accDepositType === 1 ? 0 : c.accInitialDeposit
        );

        return `
            <div class="app-card calc-card">
                <div class="calc-card-title">적립 및 예탁 미래가치 계산기</div>
                <div class="calc-card-desc">매월 적립식 저축과 목돈 거치식 예탁의 복리 증식 가치를 산출합니다.</div>

                <div class="calc-inputs-grid">
                    <div class="calc-input-group">
                        <label>초기 거치 예탁금 (원)</label>
                        <input type="number" class="calc-input" value="${c.accInitialDeposit}"
                               oninput="AppSimulator.updateCalcParam('accInitialDeposit', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>매월 추가 적립액 (원)</label>
                        <input type="number" class="calc-input" value="${c.accMonthlyDeposit}"
                               oninput="AppSimulator.updateCalcParam('accMonthlyDeposit', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>투자/적립 기간 (년)</label>
                        <input type="number" class="calc-input" value="${c.accPeriodYears}"
                               oninput="AppSimulator.updateCalcParam('accPeriodYears', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>연간 복리 수익률 (%)</label>
                        <input type="number" step="0.1" class="calc-input" value="${c.accInterestRate}"
                               oninput="AppSimulator.updateCalcParam('accInterestRate', this.value)">
                    </div>
                </div>

                <div class="calc-result-box">
                    <div class="result-label">${c.accPeriodYears}년 후 만기 예상 미래가치</div>
                    <div class="result-big-val text-emerald">${CurrencyFormatter.formatKoreanWon(res.futureValue)}</div>
                    <div class="result-detail-row">
                        <span>원금 합계: ${CurrencyFormatter.formatKoreanWon(res.totalPrincipal, true)}</span>
                        <span>이자 수익: <strong class="text-emerald">+${CurrencyFormatter.formatKoreanWon(res.totalInterest, true)}</strong></span>
                    </div>
                </div>
            </div>
        `;
    },

    // 계산기 모드 2: 자산 인출 시뮬레이션
    getCalcWithdrawalHTML() {
        const c = this.state.calc;
        const res = CalculatorsEngine.calculateWithdrawal(
            c.withStartingWealth,
            c.withAnnualGrowthRate,
            c.withIsFixedAmount,
            c.withMonthlyWithdrawal,
            c.withAnnualWithdrawalRate,
            60,
            35
        );

        return `
            <div class="app-card calc-card">
                <div class="calc-card-title">자산 인출 시뮬레이션</div>
                <div class="calc-card-desc">은퇴자산 인출 방식(정액 vs 정률)에 따른 자산 잔액 추이를 분석합니다.</div>

                <div class="calc-inputs-grid">
                    <div class="calc-input-group">
                        <label>은퇴 시작 자산 (원)</label>
                        <input type="number" class="calc-input" value="${c.withStartingWealth}"
                               oninput="AppSimulator.updateCalcParam('withStartingWealth', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>운용 기대수익률 (%)</label>
                        <input type="number" step="0.1" class="calc-input" value="${c.withAnnualGrowthRate}"
                               oninput="AppSimulator.updateCalcParam('withAnnualGrowthRate', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>월 정액 인출액 (원)</label>
                        <input type="number" class="calc-input" value="${c.withMonthlyWithdrawal}"
                               oninput="AppSimulator.updateCalcParam('withMonthlyWithdrawal', this.value)">
                    </div>
                </div>

                <div class="calc-result-box">
                    <div class="result-label">35년 인출 시뮬레이션 결과</div>
                    <div class="result-big-val ${res.isDepleted ? 'text-danger' : 'text-emerald'}">
                        ${res.isDepleted ? `${res.depletionAge}세 자산 고갈` : '35년 후에도 자산 유지'}
                    </div>
                    <div class="result-detail-row">
                        <span>95세 최종 잔액: ${CurrencyFormatter.formatKoreanWon(res.finalBalance, true)}</span>
                    </div>
                </div>
            </div>
        `;
    },

    // 계산기 모드 3: 고갈 타이머
    getCalcDepletionHTML() {
        const c = this.state.calc;
        const res = CalculatorsEngine.calculateDepletion(
            c.depCurrentWealth,
            c.depMonthlyWithdrawal,
            c.depAnnualGrowthRate,
            c.depInflationRate,
            60
        );

        return `
            <div class="app-card calc-card">
                <div class="calc-card-title">자산 고갈 타이머</div>
                <div class="calc-card-desc">현재 보유 자산으로 희망 생활비를 지출할 때 몇 년 동안 버틸 수 있는지 계산합니다.</div>

                <div class="calc-inputs-grid">
                    <div class="calc-input-group">
                        <label>보유 은퇴자산 (원)</label>
                        <input type="number" class="calc-input" value="${c.depCurrentWealth}"
                               oninput="AppSimulator.updateCalcParam('depCurrentWealth', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>월 희망 인출액 (원)</label>
                        <input type="number" class="calc-input" value="${c.depMonthlyWithdrawal}"
                               oninput="AppSimulator.updateCalcParam('depMonthlyWithdrawal', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>자산 수익률 (%)</label>
                        <input type="number" step="0.1" class="calc-input" value="${c.depAnnualGrowthRate}"
                               oninput="AppSimulator.updateCalcParam('depAnnualGrowthRate', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>물가상승률 (%)</label>
                        <input type="number" step="0.1" class="calc-input" value="${c.depInflationRate}"
                               oninput="AppSimulator.updateCalcParam('depInflationRate', this.value)">
                    </div>
                </div>

                <div class="calc-result-box">
                    <div class="result-label">자산 수명 진단</div>
                    <div class="result-big-val ${res.isForeverSafe ? 'text-emerald' : 'text-amber'}">
                        ${res.isForeverSafe ? '평생 영구 보존 (안전)' : `${res.depletionYears.toFixed(1)}년 버팀 (${res.depletionAge.toFixed(0)}세 고갈)`}
                    </div>
                    <div class="result-detail-row">
                        <span>원금 보존 안전 인출액: <strong>${CurrencyFormatter.formatKoreanWon(res.recommendedSafeMonthlyWithdrawal)}/월</strong></span>
                    </div>
                </div>
            </div>
        `;
    },

    // 계산기 모드 4: 목표 필요자산 역산
    getCalcRequiredWealthHTML() {
        const c = this.state.calc;
        const res = CalculatorsEngine.calculateRequiredWealth(
            c.reqDesiredMonthlyExpense,
            c.reqRetirementYears,
            c.reqExpectedReturnRate,
            c.reqCurrentAge,
            c.reqTargetRetirementAge
        );

        return `
            <div class="app-card calc-card">
                <div class="calc-card-title">목표 필요자산 역산</div>
                <div class="calc-card-desc">은퇴 후 원하는 월 생활비를 유지하기 위해 총 얼마가 필요하고 매월 얼마를 모아야 하는지 역산합니다.</div>

                <div class="calc-inputs-grid">
                    <div class="calc-input-group">
                        <label>은퇴 후 월 희망 생활비 (원)</label>
                        <input type="number" class="calc-input" value="${c.reqDesiredMonthlyExpense}"
                               oninput="AppSimulator.updateCalcParam('reqDesiredMonthlyExpense', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>은퇴 기간 (년)</label>
                        <input type="number" class="calc-input" value="${c.reqRetirementYears}"
                               oninput="AppSimulator.updateCalcParam('reqRetirementYears', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>현재 나이 / 은퇴 희망 나이</label>
                        <div style="display:flex; gap:6px;">
                            <input type="number" class="calc-input" value="${c.reqCurrentAge}" style="width:50%"
                                   oninput="AppSimulator.updateCalcParam('reqCurrentAge', this.value)">
                            <input type="number" class="calc-input" value="${c.reqTargetRetirementAge}" style="width:50%"
                                   oninput="AppSimulator.updateCalcParam('reqTargetRetirementAge', this.value)">
                        </div>
                    </div>
                </div>

                <div class="calc-result-box">
                    <div class="result-label">은퇴 시점 필요 총자산</div>
                    <div class="result-big-val text-emerald">${CurrencyFormatter.formatKoreanWon(res.targetTotalWealth)}</div>
                    <div class="result-detail-row">
                        <span>현재부터 월 필요 저축액: <strong class="text-cyan">${CurrencyFormatter.formatKoreanWon(res.monthlySavingsNeeded)}/월</strong></span>
                    </div>
                </div>
            </div>
        `;
    },

    // 계산기 모드 5: 국민연금 조기/연기 손익분기
    getCalcBreakEvenHTML() {
        const c = this.state.calc;
        const res = CalculatorsEngine.calculateNationalPensionBreakEven(
            c.beNormalMonthlyAmount,
            c.beStatutoryAge,
            this.state.profile.policySettings
        );

        return `
            <div class="app-card calc-card">
                <div class="calc-card-title">국민연금 조기 vs 정상 vs 연기 손익분기</div>
                <div class="calc-card-desc">언제 수령하는 것이 생애 총 수령액 관점에서 가장 유리한지 교차 분기점을 계산합니다.</div>

                <div class="calc-inputs-grid">
                    <div class="calc-input-group">
                        <label>정상 수령 시 월 국민연금액 (원)</label>
                        <input type="number" class="calc-input" value="${c.beNormalMonthlyAmount}"
                               oninput="AppSimulator.updateCalcParam('beNormalMonthlyAmount', this.value)">
                    </div>
                </div>

                <div class="calc-result-box">
                    <div class="result-label">황금 손익분기점 (Break-Even Age)</div>
                    <div class="break-even-points-row">
                        <div class="be-point">
                            <div class="be-label">조기(60세) vs 정상(65세)</div>
                            <div class="be-age text-emerald">만 ${res.earlyVsNormalBreakEvenAge}세</div>
                            <div class="be-desc">${res.earlyVsNormalBreakEvenAge}세 이전 사망 시 조기 유리, 이후 생존 시 정상수령 역전</div>
                        </div>
                        <div class="be-point">
                            <div class="be-label">정상(65세) vs 연기(70세)</div>
                            <div class="be-age text-cyan">만 ${res.normalVsDelayedBreakEvenAge}세</div>
                            <div class="be-desc">${res.normalVsDelayedBreakEvenAge}세 이상 장수 시 연기수령 총수령액 압도적 우위</div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    },

    // 계산기 모드 6: 연금저축/IRP 절세 연금술사
    getCalcTaxBenefitHTML() {
        const c = this.state.calc;
        const res = CalculatorsEngine.calculateTaxBenefit(
            c.taxAnnualContribution,
            c.taxIsLowIncomeTier,
            this.state.profile.policySettings
        );

        return `
            <div class="app-card calc-card">
                <div class="calc-card-title">연금저축/IRP 절세 연금술사</div>
                <div class="calc-card-desc">연간 최대 900만원 세액공제 환급금과 과세이연 복리 재투자의 기적을 계산합니다.</div>

                <div class="calc-inputs-grid">
                    <div class="calc-input-group">
                        <label>연간 연금계좌 납입액 (원, 최대 900만)</label>
                        <input type="number" class="calc-input" value="${c.taxAnnualContribution}"
                               oninput="AppSimulator.updateCalcParam('taxAnnualContribution', this.value)">
                    </div>
                    <div class="calc-input-group">
                        <label>총급여 구분</label>
                        <select class="calc-input" onchange="AppSimulator.updateCalcParam('taxIsLowIncomeTier', this.value === 'true')">
                            <option value="true" ${c.taxIsLowIncomeTier ? 'selected' : ''}>5,500만원 이하 (16.5% 공제)</option>
                            <option value="false" ${!c.taxIsLowIncomeTier ? 'selected' : ''}>5,500만원 초과 (13.2% 공제)</option>
                        </select>
                    </div>
                </div>

                <div class="calc-result-box">
                    <div class="result-label">연간 연말정산 환급금</div>
                    <div class="result-big-val text-emerald">${CurrencyFormatter.formatKoreanWon(res.taxRefundAmount)} / 매년 환급</div>
                    <div class="result-detail-row">
                        <span>30년 환급금 단순 합계: ${CurrencyFormatter.formatKoreanWon(res.cumulativeTaxRefund30Years, true)}</span>
                        <span>연 6% 복리 재투자 시 30년 후: <strong class="text-emerald">${CurrencyFormatter.formatKoreanWon(res.reinvestedFutureValue30Years, true)}</strong></span>
                    </div>
                    <div class="tax-bonus-banner">
                        ✨ 일반 과세계좌 대비 절세 복리 보너스: <strong>+${CurrencyFormatter.formatKoreanWon(res.taxSavingsAlchemyBonus, true)}</strong>
                    </div>
                </div>
            </div>
        `;
    },

    updateCalcParam(key, value) {
        if (key === 'taxIsLowIncomeTier') {
            this.state.calc[key] = Boolean(value);
        } else {
            this.state.calc[key] = Number(value);
        }
        this.renderAppScreen();
    },

    // ──────────────────────────────────────────────
    // 5. 환경 설정 화면 HTML
    // ──────────────────────────────────────────────
    getSettingsHTML() {
        const p = this.state.profile;
        const pol = p.policySettings;

        return `
            <div class="screen-scroll-container">
                <!-- 상단 프리셋 칩 바 -->
                <div class="preset-chips-box">
                    <div class="preset-chips-title">생애주기 원터치 프리셋</div>
                    <div class="preset-chips-row">
                        <button class="preset-chip-btn" onclick="AppSimulator.loadPreset('preset20s')">20대 청년</button>
                        <button class="preset-chip-btn" onclick="AppSimulator.loadPreset('preset30s')">30대 형성</button>
                        <button class="preset-chip-btn" onclick="AppSimulator.loadPreset('preset40s')">40대 기본</button>
                        <button class="preset-chip-btn" onclick="AppSimulator.loadPreset('preset50s')">50대 가속</button>
                        <button class="preset-chip-btn" onclick="AppSimulator.loadPreset('preset60s')">60대 은퇴</button>
                    </div>
                </div>

                <!-- 탭 전환: 기본 프로필 vs 세법 정책 변수 -->
                <div class="app-tab-switch" style="margin-top:12px;">
                    <button class="tab-btn ${this.state.settingsTab === 0 ? 'active' : ''}" onclick="AppSimulator.setSettingsTab(0)">
                        기본 프로필
                    </button>
                    <button class="tab-btn ${this.state.settingsTab === 1 ? 'active' : ''}" onclick="AppSimulator.setSettingsTab(1)">
                        세법·정책 변수(상수)
                    </button>
                </div>

                ${this.state.settingsTab === 0 ? `
                <div class="app-card settings-card">
                    <div class="calc-inputs-grid">
                        <div class="calc-input-group">
                            <label>출생 연도 (현재 나이: ${p.currentAge}세)</label>
                            <input type="number" class="calc-input" value="${p.birthYear}"
                                   onchange="AppSimulator.updateProfileParam('birthYear', this.value)">
                        </div>
                        <div class="calc-input-group">
                            <label>은퇴 희망 나이 (세)</label>
                            <input type="number" class="calc-input" value="${p.retirementAge}"
                                   onchange="AppSimulator.updateProfileParam('retirementAge', this.value)">
                        </div>
                        <div class="calc-input-group">
                            <label>은퇴 후 월 희망 생활비 (만원)</label>
                            <input type="number" class="calc-input" value="${p.monthlyExpenses / 10000}"
                                   onchange="AppSimulator.updateProfileParam('monthlyExpenses', this.value * 10000)">
                        </div>
                        <div class="calc-input-group">
                            <label>연간 물가상승률 (%)</label>
                            <input type="number" step="0.1" class="calc-input" value="${p.inflationRate}"
                                   onchange="AppSimulator.updateProfileParam('inflationRate', this.value)">
                        </div>
                    </div>
                </div>
                ` : `
                <div class="app-card settings-card">
                    <div class="calc-card-desc">세법 개정 및 정부 정책 변경 시 사용자가 직접 상수를 조정할 수 있습니다.</div>
                    <div class="calc-inputs-grid">
                        <div class="calc-input-group">
                            <label>사적연금 저율분리과세 연한도 (만원)</label>
                            <input type="number" class="calc-input" value="${pol.privatePensionAnnualLimit / 10000}"
                                   onchange="AppSimulator.updatePolicyParam('privatePensionAnnualLimit', this.value * 10000)">
                        </div>
                        <div class="calc-input-group">
                            <label>건보료 피부양자 연금한도 (만원)</label>
                            <input type="number" class="calc-input" value="${pol.healthInsurancePensionLimit / 10000}"
                                   onchange="AppSimulator.updatePolicyParam('healthInsurancePensionLimit', this.value * 10000)">
                        </div>
                        <div class="calc-input-group">
                            <label>연금저축/IRP 세액공제 납입한도 (만원)</label>
                            <input type="number" class="calc-input" value="${pol.maxTaxCreditContribution / 10000}"
                                   onchange="AppSimulator.updatePolicyParam('maxTaxCreditContribution', this.value * 10000)">
                        </div>
                        <div class="calc-input-group">
                            <label>국민연금 조기감액률/연기증액률 (%/년)</label>
                            <div style="display:flex; gap:6px;">
                                <input type="number" step="0.1" class="calc-input" value="${pol.nationalEarlyReductionRatePerYear}" style="width:50%"
                                       onchange="AppSimulator.updatePolicyParam('nationalEarlyReductionRatePerYear', this.value)">
                                <input type="number" step="0.1" class="calc-input" value="${pol.nationalDelayIncreaseRatePerYear}" style="width:50%"
                                       onchange="AppSimulator.updatePolicyParam('nationalDelayIncreaseRatePerYear', this.value)">
                            </div>
                        </div>
                    </div>
                </div>
                `}

                <!-- 데이터 초기화 및 백업 안내 -->
                <div class="app-card action-card">
                    <div class="action-btn-row">
                        <button class="action-btn btn-secondary" onclick="AppSimulator.exportJSON()">📤 JSON 백업 내보내기</button>
                        <button class="action-btn btn-danger" onclick="AppSimulator.resetData()">🔄 기본값 초기화</button>
                    </div>
                </div>

                <div style="height: 60px;"></div>
            </div>
        `;
    },

    setSettingsTab(idx) {
        this.state.settingsTab = idx;
        this.renderAppScreen();
    },

    updateProfileParam(key, val) {
        this.state.profile[key] = Number(val);
        this.runSimulation();
        this.renderAll();
    },

    updatePolicyParam(key, val) {
        this.state.profile.policySettings[key] = Number(val);
        this.runSimulation();
        this.renderAll();
    },

    exportJSON() {
        const data = {
            profile: this.state.profile,
            pensions: this.state.pensions,
            assets: this.state.assets,
            incomes: this.state.incomes,
            exportDate: new Date().toISOString()
        };
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `PensionAlchemy_Backup_${new Date().toISOString().slice(0, 10)}.json`;
        a.click();
        URL.revokeObjectURL(url);
    },

    resetData() {
        if (confirm('모든 데이터를 40대 기본 프리셋으로 초기화하시겠습니까?')) {
            this.loadPreset('preset40s');
        }
    },

    // ──────────────────────────────────────────────
    // 6. 가이드 & 도움말 화면 HTML
    // ──────────────────────────────────────────────
    getGuideHTML() {
        const cat = this.state.guideCategory;
        const categories = ["앱 사용 도움말", "연금술사 재무비법", "정부 공식 포털", "추천 전문 채널"];

        return `
            <div class="screen-scroll-container">
                <div class="app-tab-switch">
                    ${categories.map((c, i) => `
                        <button class="tab-btn ${cat === i ? 'active' : ''}" onclick="AppSimulator.setGuideCategory(${i})">
                            ${c}
                        </button>
                    `).join('')}
                </div>

                <div class="guide-content-area">
                    ${cat === 0 ? `
                        <div class="guide-card">
                            <div class="guide-card-title">💡 연금술사 핵심 기능 안내</div>
                            <div class="guide-tip-item"><strong>1. 대시보드:</strong> 순자산, 65세 월연금, 건강 점수 및 나이별 인스펙터 슬라이더</div>
                            <div class="guide-tip-item"><strong>2. 자산관리:</strong> 예금, 적금, 주식, 부동산 및 부채 4대 상환방식 관리</div>
                            <div class="guide-tip-item"><strong>3. 연금플랜:</strong> 3층 연금 피라미드 및 국민연금 조기(-30%)/연기(+36%) 슬라이더</div>
                            <div class="guide-tip-item"><strong>4. 6대 계산기:</strong> 미래가치, 인출, 고갈 타이머, 목표자산, 손익분기, 절세 연금술</div>
                            <div class="guide-tip-item"><strong>5. 환경설정:</strong> 5대 생애주기 프리셋 및 세법/정책 변수 커스텀</div>
                        </div>
                    ` : cat === 1 ? `
                        <div class="guide-card">
                            <div class="guide-card-badge">비법 1</div>
                            <div class="guide-card-title">3층 연금 피라미드 황금비율 구축</div>
                            <div class="guide-card-summary">기본 생계는 1층 국민연금, 표준 생활은 2층 퇴직연금, 여유 생활은 3층 개인연금으로 방어망을 구축하고 주택연금(역모기지)을 평생 안전판으로 결합하세요.</div>
                        </div>
                        <div class="guide-card">
                            <div class="guide-card-badge">비법 2</div>
                            <div class="guide-card-title">마의 5년 '소득 크레바스' 방어 전략</div>
                            <div class="guide-card-summary">정년(60세)부터 국민연금 수령(63~65세)까지의 소득 공백기를 버틸 개인연금/IRP 브릿지 자금을 사전에 확보하세요.</div>
                        </div>
                        <div class="guide-card">
                            <div class="guide-card-badge">비법 3</div>
                            <div class="guide-card-title">글로벌 '4% 안전 인출 룰'</div>
                            <div class="guide-card-summary">미국 트리니티 스터디에 따르면 첫해 자산의 4%를 인출하고 매년 물가상승률만큼 증액 인출 시 30년 이상 자산 고갈 위험이 95% 이상 방어됩니다.</div>
                        </div>
                    ` : cat === 2 ? `
                        <div class="guide-link-card" onclick="window.open('https://csa.nps.or.kr', '_blank')">
                            <div class="guide-link-tag tag-blue">1층 국민연금</div>
                            <div class="guide-link-title">국민연금공단 (NPS) - 내 연금 알아보기 ↗</div>
                            <div class="guide-link-desc">예상 수령액, 납부 총 보험료 내역 및 노령연금 모의계산 실시간 조회</div>
                        </div>
                        <div class="guide-link-card" onclick="window.open('https://www.fss.or.kr/fss/lifeplan/lifeplanIndex/index.do?menuNo=201101', '_blank')">
                            <div class="guide-link-tag tag-cyan">통합 연금 조회</div>
                            <div class="guide-link-title">금융감독원 통합연금포털 ↗</div>
                            <div class="guide-link-desc">국민·퇴직·개인연금(은행·보험·증권)을 한 화면에서 한 번에 통합 조회</div>
                        </div>
                        <div class="guide-link-card" onclick="window.open('https://www.hf.go.kr', '_blank')">
                            <div class="guide-link-tag tag-amber">주택연금/역모기지</div>
                            <div class="guide-link-title">한국주택금융공사 (주택연금) ↗</div>
                            <div class="guide-link-desc">만 55세 이상 주택 담보 평생 연금 예상 월지급금 모의계산</div>
                        </div>
                    ` : `
                        <div class="guide-link-card" onclick="window.open('https://www.youtube.com/@gomhee', '_blank')">
                            <div class="guide-link-tag tag-danger">YouTube</div>
                            <div class="guide-link-title">박곰희TV (구독자 63만) ↗</div>
                            <div class="guide-link-desc">연금저축 · IRP · 미국 ETF · 자산배분 초보 가이드 1위 채널</div>
                        </div>
                        <div class="guide-link-card" onclick="window.open('https://www.youtube.com/@pension500', '_blank')">
                            <div class="guide-link-tag tag-danger">YouTube</div>
                            <div class="guide-link-title">연금박사 이영주 소장 (구독자 28만) ↗</div>
                            <div class="guide-link-desc">은퇴설계, 국민연금 수령시기 및 건강보험료 피부양자 탈락 방어</div>
                        </div>
                    `}
                </div>

                <div style="height: 60px;"></div>
            </div>
        `;
    },

    setGuideCategory(cat) {
        this.state.guideCategory = cat;
        this.renderAppScreen();
    },

    // ──────────────────────────────────────────────
    // 모달 다이얼로그 (자산 추가, 연금 추가, 소득 추가)
    // ──────────────────────────────────────────────
    getOrCreateModalContainer() {
        let container = document.getElementById('phone-modal-overlay');
        if (!container) {
            container = document.createElement('div');
            container.id = 'phone-modal-overlay';
            container.className = 'phone-modal-overlay';
            const frame = document.querySelector('.phone-frame');
            if (frame) {
                frame.appendChild(container);
            } else {
                document.body.appendChild(container);
            }
        }
        return container;
    },

    closeModal() {
        const container = document.getElementById('phone-modal-overlay');
        if (container) {
            container.style.display = 'none';
            container.innerHTML = '';
        }
    },

    openAddAssetModal() {
        const modalContainer = this.getOrCreateModalContainer();
        modalContainer.innerHTML = `
            <div class="sim-modal-card">
                <div class="sim-modal-header">
                    <span class="sim-modal-title">자산 또는 부채 추가</span>
                    <button class="sim-modal-close" onclick="AppSimulator.closeModal()">✕</button>
                </div>
                <div class="sim-modal-body">
                    <div class="calc-input-group">
                        <label>항목 이름</label>
                        <input type="text" id="modal-asset-name" class="calc-input" placeholder="예: 미국 테크주, 전세대출 등" value="신규 자산">
                    </div>
                    <div class="calc-input-group">
                        <label>자산/부채 종류</label>
                        <select id="modal-asset-type" class="calc-input" onchange="AppSimulator.onAssetTypeChanged(this.value)">
                            <option value="STOCK">주식 (Stock)</option>
                            <option value="ETF">ETF / 펀드</option>
                            <option value="DEPOSIT">정기예금</option>
                            <option value="SAVINGS">적금</option>
                            <option value="REAL_ESTATE">부동산 (아파트/빌라 등)</option>
                            <option value="BOND">채권</option>
                            <option value="CRYPTO">가상자산</option>
                            <option value="DEBT">대출 / 부채 (Debt)</option>
                            <option value="OTHER">기타 자산</option>
                        </select>
                    </div>
                    <div class="calc-input-group">
                        <label>현재 평가금액 (원)</label>
                        <input type="number" id="modal-asset-value" class="calc-input" placeholder="원 단위 입력" value="30000000">
                    </div>
                    <div class="calc-input-group">
                        <label id="modal-asset-rate-label">기대 연수익률 (%)</label>
                        <input type="number" step="0.1" id="modal-asset-rate" class="calc-input" value="5.0">
                    </div>
                    <div id="modal-debt-fields" style="display:none;">
                        <div class="calc-input-group">
                            <label>상환 방식</label>
                            <select id="modal-debt-repayment" class="calc-input">
                                <option value="EQUAL_PRINCIPAL_AND_INTEREST">원리금균등분할상환</option>
                                <option value="EQUAL_PRINCIPAL">원금균등분할상환</option>
                                <option value="BULLET">만기일시상환</option>
                                <option value="INTEREST_ONLY">거치식 (이자만 납부)</option>
                            </select>
                        </div>
                        <div class="calc-input-group">
                            <label>대출 만기 (년)</label>
                            <input type="number" id="modal-debt-maturity" class="calc-input" value="10">
                        </div>
                    </div>
                </div>
                <div class="sim-modal-footer">
                    <button class="action-btn btn-secondary" onclick="AppSimulator.closeModal()">취소</button>
                    <button class="action-btn btn-primary" onclick="AppSimulator.submitAddAsset()">등록 완료</button>
                </div>
            </div>
        `;
        modalContainer.style.display = 'flex';
    },

    onAssetTypeChanged(type) {
        const debtFields = document.getElementById('modal-debt-fields');
        const rateLabel = document.getElementById('modal-asset-rate-label');
        if (type === 'DEBT') {
            if (debtFields) debtFields.style.display = 'block';
            if (rateLabel) rateLabel.textContent = '대출 금리 (%)';
        } else {
            if (debtFields) debtFields.style.display = 'none';
            if (rateLabel) rateLabel.textContent = '기대 연수익률 (%)';
        }
    },

    submitAddAsset() {
        const name = document.getElementById('modal-asset-name')?.value.trim() || '신규 자산';
        const type = document.getElementById('modal-asset-type')?.value || 'DEPOSIT';
        const value = Number(document.getElementById('modal-asset-value')?.value) || 0;
        const rate = Number(document.getElementById('modal-asset-rate')?.value) || 0;
        const repayment = document.getElementById('modal-debt-repayment')?.value || 'EQUAL_PRINCIPAL_AND_INTEREST';
        const maturity = Number(document.getElementById('modal-debt-maturity')?.value) || 10;
        const isLiability = type === 'DEBT';

        const newAsset = new Asset({
            name,
            type,
            currentValue: value,
            expectedGrowthRate: rate,
            repaymentMethod: repayment,
            maturityYears: maturity,
            isLiability
        });

        this.state.assets.push(newAsset);
        this.closeModal();
        this.runSimulation();
        this.renderAll();
    },

    openAddIncomeModal() {
        const modalContainer = this.getOrCreateModalContainer();
        modalContainer.innerHTML = `
            <div class="sim-modal-card">
                <div class="sim-modal-header">
                    <span class="sim-modal-title">정기 소득 추가</span>
                    <button class="sim-modal-close" onclick="AppSimulator.closeModal()">✕</button>
                </div>
                <div class="sim-modal-body">
                    <div class="calc-input-group">
                        <label>소득 명칭</label>
                        <input type="text" id="modal-income-name" class="calc-input" placeholder="예: 추가 부업, 임대소득 등" value="신규 부업소득">
                    </div>
                    <div class="calc-input-group">
                        <label>소득 유형</label>
                        <select id="modal-income-type" class="calc-input">
                            <option value="SALARY">근로소득</option>
                            <option value="BUSINESS">사업소득</option>
                            <option value="RENTAL">임대소득</option>
                            <option value="BONUS">상여/성과급</option>
                            <option value="OTHER">기타소득</option>
                        </select>
                    </div>
                    <div class="calc-input-group">
                        <label>월 수령액 (원)</label>
                        <input type="number" id="modal-income-amount" class="calc-input" value="1000000">
                    </div>
                    <div class="calc-inputs-grid">
                        <div class="calc-input-group">
                            <label>소득 종료 나이 (세)</label>
                            <input type="number" id="modal-income-end" class="calc-input" value="60">
                        </div>
                        <div class="calc-input-group">
                            <label>연간 소득상승률 (%)</label>
                            <input type="number" step="0.1" id="modal-income-rate" class="calc-input" value="2.0">
                        </div>
                    </div>
                </div>
                <div class="sim-modal-footer">
                    <button class="action-btn btn-secondary" onclick="AppSimulator.closeModal()">취소</button>
                    <button class="action-btn btn-primary" onclick="AppSimulator.submitAddIncome()">소득 등록</button>
                </div>
            </div>
        `;
        modalContainer.style.display = 'flex';
    },

    submitAddIncome() {
        const name = document.getElementById('modal-income-name')?.value.trim() || '신규 소득';
        const type = document.getElementById('modal-income-type')?.value || 'SALARY';
        const amount = Number(document.getElementById('modal-income-amount')?.value) || 0;
        const endAge = Number(document.getElementById('modal-income-end')?.value) || 60;
        const rate = Number(document.getElementById('modal-income-rate')?.value) || 0;

        const newIncome = new Income({
            name,
            type,
            monthlyAmount: amount,
            endAge,
            expectedGrowthRate: rate
        });

        this.state.incomes.push(newIncome);
        this.closeModal();
        this.runSimulation();
        this.renderAll();
    },

    openAddPensionModal() {
        const modalContainer = this.getOrCreateModalContainer();
        modalContainer.innerHTML = `
            <div class="sim-modal-card">
                <div class="sim-modal-header">
                    <span class="sim-modal-title">새 연금 플랜 추가</span>
                    <button class="sim-modal-close" onclick="AppSimulator.closeModal()">✕</button>
                </div>
                <div class="sim-modal-body">
                    <div class="calc-input-group">
                        <label>연금 명칭</label>
                        <input type="text" id="modal-pension-name" class="calc-input" placeholder="예: 개인연금저축(신한), 퇴직IRP 등" value="신규 개인연금">
                    </div>
                    <div class="calc-input-group">
                        <label>연금 유형</label>
                        <select id="modal-pension-type" class="calc-input">
                            <option value="PERSONAL">3층 개인연금저축 (세제적격)</option>
                            <option value="RETIREMENT">2층 퇴직연금 / IRP</option>
                            <option value="ANNUITY_INSURANCE">3층 개인연금보험 (비과세)</option>
                            <option value="HOUSING">주택연금 (역모기지)</option>
                            <option value="OTHER">기타 연금</option>
                        </select>
                    </div>
                    <div class="calc-inputs-grid">
                        <div class="calc-input-group">
                            <label>수령 시작 나이</label>
                            <input type="number" id="modal-pension-start" class="calc-input" value="60">
                        </div>
                        <div class="calc-input-group">
                            <label>수령 종료 나이</label>
                            <input type="number" id="modal-pension-end" class="calc-input" value="85">
                        </div>
                    </div>
                    <div class="calc-input-group">
                        <label>예상 월 수령액 (원)</label>
                        <input type="number" id="modal-pension-amount" class="calc-input" value="500000">
                    </div>
                    <div class="calc-inputs-grid">
                        <div class="calc-input-group">
                            <label>현재 적립금 (원)</label>
                            <input type="number" id="modal-pension-bal" class="calc-input" value="10000000">
                        </div>
                        <div class="calc-input-group">
                            <label>월 납입액 (원)</label>
                            <input type="number" id="modal-pension-contrib" class="calc-input" value="200000">
                        </div>
                    </div>
                    <div class="calc-input-group">
                        <label>운용 기대수익률 (%)</label>
                        <input type="number" step="0.1" id="modal-pension-rate" class="calc-input" value="5.0">
                    </div>
                </div>
                <div class="sim-modal-footer">
                    <button class="action-btn btn-secondary" onclick="AppSimulator.closeModal()">취소</button>
                    <button class="action-btn btn-primary" onclick="AppSimulator.submitAddPension()">연금 등록</button>
                </div>
            </div>
        `;
        modalContainer.style.display = 'flex';
    },

    submitAddPension() {
        const name = document.getElementById('modal-pension-name')?.value.trim() || '신규 연금';
        const type = document.getElementById('modal-pension-type')?.value || 'PERSONAL';
        const startAge = Number(document.getElementById('modal-pension-start')?.value) || 60;
        const endAge = Number(document.getElementById('modal-pension-end')?.value) || 85;
        const expectedMonthlyAmount = Number(document.getElementById('modal-pension-amount')?.value) || 0;
        const currentBalance = Number(document.getElementById('modal-pension-bal')?.value) || 0;
        const monthlyContribution = Number(document.getElementById('modal-pension-contrib')?.value) || 0;
        const expectedGrowthRate = Number(document.getElementById('modal-pension-rate')?.value) || 0;

        const newPension = new Pension({
            name,
            type,
            startAge,
            endAge,
            expectedMonthlyAmount,
            currentBalance,
            monthlyContribution,
            expectedGrowthRate,
            contributionEndAge: startAge,
            isTaxDeductionEligible: type === 'PERSONAL' || type === 'RETIREMENT'
        });

        this.state.pensions.push(newPension);
        this.closeModal();
        this.runSimulation();
        this.renderAll();
    },

    deleteAsset(id) {
        if (confirm('해당 자산/부채 항목을 삭제하시겠습니까?')) {
            this.state.assets = this.state.assets.filter(a => a.id !== id);
            this.runSimulation();
            this.renderAll();
        }
    },

    deleteIncome(id) {
        if (confirm('해당 소득 항목을 삭제하시겠습니까?')) {
            this.state.incomes = this.state.incomes.filter(i => i.id !== id);
            this.runSimulation();
            this.renderAll();
        }
    },

    deletePension(id) {
        if (confirm('해당 연금 항목을 삭제하시겠습니까?')) {
            this.state.pensions = this.state.pensions.filter(p => p.id !== id);
            this.runSimulation();
            this.renderAll();
        }
    },

    // ──────────────────────────────────────────────
    // 캔버스 차트 렌더링 엔진 (Canvas Charts)
    // ──────────────────────────────────────────────
    initDashboardCharts() {
        setTimeout(() => {
            this.drawTrajectoryChart();
            this.drawCashFlowStackChart();
            this.drawDonutChart();
            this.drawDistributionChart();
        }, 50);
    },

    drawTrajectoryChart() {
        const canvas = document.getElementById('canvas-trajectory');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * (window.devicePixelRatio || 1);
        canvas.height = 220 * (window.devicePixelRatio || 1);
        ctx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);

        const width = rect.width > 0 ? rect.width : (canvas.parentElement ? canvas.parentElement.clientWidth : 340);
        const height = 220;
        const s = this.state.summary;
        const results = s.yearlyResults;
        if (!results || results.length === 0) return;

        ctx.clearRect(0, 0, width, height);

        const isLight = document.documentElement.getAttribute('data-theme') === 'light';
        const gridColor = isLight ? 'rgba(100, 116, 139, 0.22)' : 'rgba(148, 163, 184, 0.15)';
        const textColor = isLight ? '#475569' : 'rgba(148, 163, 184, 0.8)';

        const padding = { top: 20, right: 20, bottom: 30, left: 55 };
        const chartW = width - padding.left - padding.right;
        const chartH = height - padding.top - padding.bottom;

        // 최대값 및 최저값 산출 (마이너스 순자산 동적 지원)
        const maxVal = Math.max(...results.map(r => r.totalGrossAssets), 100000000);
        const minValRaw = Math.min(0, ...results.map(r => r.netAssetValue));
        const minVal = minValRaw < 0 ? minValRaw * 1.15 : 0;
        const valRange = (maxVal - minVal) || 1;

        const minAge = results[0].age;
        const maxAge = results[results.length - 1].age;

        const getX = (age) => padding.left + ((age - minAge) / (maxAge - minAge)) * chartW;
        const getY = (val) => padding.top + chartH - ((val - minVal) / valRange) * chartH;
        const zeroY = getY(0);

        // 배경 그리드 라인
        ctx.strokeStyle = gridColor;
        ctx.lineWidth = 1;
        for (let i = 0; i <= 4; i++) {
            const y = padding.top + (chartH / 4) * i;
            ctx.beginPath();
            ctx.moveTo(padding.left, y);
            ctx.lineTo(padding.left + chartW, y);
            ctx.stroke();

            const v = minVal + valRange * (1 - i / 4);
            ctx.fillStyle = textColor;
            ctx.font = '10px Inter, sans-serif';
            ctx.textAlign = 'right';
            ctx.fillText(CurrencyFormatter.formatKoreanWon(v, true), padding.left - 6, y + 4);
        }

        // 0원 손익분기 기준선 (Dashed Line)
        if (minValRaw < 0 && zeroY >= padding.top && zeroY <= padding.top + chartH) {
            ctx.strokeStyle = 'rgba(244, 63, 94, 0.7)';
            ctx.lineWidth = 1.5;
            ctx.setLineDash([6, 6]);
            ctx.beginPath();
            ctx.moveTo(padding.left, zeroY);
            ctx.lineTo(padding.left + chartW, zeroY);
            ctx.stroke();
            ctx.setLineDash([]);
        }

        // 은퇴 나이 음영 영역
        const retX = getX(s.retirementAge);
        ctx.fillStyle = 'rgba(99, 102, 241, 0.08)';
        ctx.fillRect(retX, padding.top, chartW - (retX - padding.left), chartH);

        // 은퇴선 세로선
        ctx.strokeStyle = 'rgba(99, 102, 241, 0.6)';
        ctx.setLineDash([4, 4]);
        ctx.beginPath();
        ctx.moveTo(retX, padding.top);
        ctx.lineTo(retX, padding.top + chartH);
        ctx.stroke();
        ctx.setLineDash([]);

        // 곡선 그리기 헬퍼
        const drawLine = (prop, strokeStyle, fillStyle) => {
            ctx.beginPath();
            results.forEach((r, idx) => {
                const x = getX(r.age);
                const y = getY(r[prop]);
                if (idx === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            });

            if (fillStyle) {
                ctx.lineTo(getX(results[results.length - 1].age), zeroY);
                ctx.lineTo(getX(results[0].age), zeroY);
                ctx.closePath();
                ctx.fillStyle = fillStyle;
                ctx.fill();
            }

            ctx.strokeStyle = strokeStyle;
            ctx.lineWidth = 2.5;
            ctx.stroke();
        };

        // 1. 총자산 (Blue)
        drawLine('totalGrossAssets', '#3B82F6', null);
        // 2. 순자산 (Emerald Fill + Stroke)
        drawLine('netAssetValue', '#10B981', 'rgba(16, 185, 129, 0.12)');
        // 3. 부채 (Rose)
        drawLine('totalDebt', '#F43F5E', null);

        // 선택된 나이 커서
        const curX = getX(this.state.selectedAge);
        ctx.strokeStyle = '#F59E0B';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(curX, padding.top);
        ctx.lineTo(curX, padding.top + chartH);
        ctx.stroke();

        // 선택된 나이 점
        const curItem = results.find(r => r.age === this.state.selectedAge) || results[0];
        const curNetY = getY(curItem.netAssetValue);
        ctx.fillStyle = '#F59E0B';
        ctx.beginPath();
        ctx.arc(curX, curNetY, 5, 0, Math.PI * 2);
        ctx.fill();

        // X축 나이 라벨
        ctx.fillStyle = textColor;
        ctx.font = '10px Inter, sans-serif';
        ctx.textAlign = 'center';
        [minAge, s.retirementAge, 75, maxAge].forEach(age => {
            ctx.fillText(`${age}세`, getX(age), padding.top + chartH + 18);
        });
    },

    drawCashFlowStackChart() {
        const canvas = document.getElementById('canvas-cashflow-stack');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * (window.devicePixelRatio || 1);
        canvas.height = 190 * (window.devicePixelRatio || 1);
        ctx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);

        const width = rect.width > 0 ? rect.width : (canvas.parentElement ? canvas.parentElement.clientWidth : 340);
        const height = 190;
        const results = this.state.summary.yearlyResults.filter(r => r.age >= 55 && r.age <= 90);
        if (results.length === 0) return;

        ctx.clearRect(0, 0, width, height);
        const padding = { top: 15, right: 15, bottom: 25, left: 45 };
        const chartW = width - padding.left - padding.right;
        const chartH = height - padding.top - padding.bottom;

        const maxMonthly = Math.max(...results.map(r => r.grossMonthlyPensionIncome), 2000000);
        const barWidth = Math.max(2, chartW / results.length - 2);

        results.forEach((r, idx) => {
            const x = padding.left + idx * (chartW / results.length);
            let currentY = padding.top + chartH;

            // 1층 국민 (Blue)
            const natH = (r.monthlyNationalPension / maxMonthly) * chartH;
            ctx.fillStyle = '#3B82F6';
            ctx.fillRect(x, currentY - natH, barWidth, natH);
            currentY -= natH;

            // 2층 퇴직 (Purple)
            const retH = (r.monthlyRetirementPension / maxMonthly) * chartH;
            ctx.fillStyle = '#8B5CF6';
            ctx.fillRect(x, currentY - retH, barWidth, retH);
            currentY -= retH;

            // 3층 개인 (Emerald)
            const perH = (r.monthlyPersonalPension / maxMonthly) * chartH;
            ctx.fillStyle = '#10B981';
            ctx.fillRect(x, currentY - perH, barWidth, perH);
            currentY -= perH;

            // 주택연금 (Amber)
            const houH = (r.monthlyHousingPension / maxMonthly) * chartH;
            ctx.fillStyle = '#F59E0B';
            ctx.fillRect(x, currentY - houH, barWidth, houH);
        });

        // X축 라벨
        ctx.fillStyle = 'rgba(148, 163, 184, 0.8)';
        ctx.font = '10px Inter, sans-serif';
        ctx.textAlign = 'center';
        [55, 65, 75, 85].forEach(age => {
            const idx = results.findIndex(r => r.age === age);
            if (idx !== -1) {
                const x = padding.left + idx * (chartW / results.length) + barWidth / 2;
                ctx.fillText(`${age}세`, x, padding.top + chartH + 16);
            }
        });
    },

    drawDonutChart() {
        const canvas = document.getElementById('canvas-donut');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const s = this.state.summary;

        const total = Math.max(1, s.currentFinancialAssets + s.currentPensionAssets + s.currentRealEstateAssets + s.currentTotalDebt);
        const slices = [
            { val: s.currentFinancialAssets, color: '#3B82F6' },
            { val: s.currentPensionAssets, color: '#10B981' },
            { val: s.currentRealEstateAssets, color: '#F59E0B' },
            { val: s.currentTotalDebt, color: '#F43F5E' }
        ];

        const cx = 80;
        const cy = 80;
        const radius = 65;
        const innerRadius = 45;

        ctx.clearRect(0, 0, 160, 160);
        let startAngle = -Math.PI / 2;

        slices.forEach(slice => {
            const sliceAngle = (slice.val / total) * Math.PI * 2;
            ctx.beginPath();
            ctx.arc(cx, cy, radius, startAngle, startAngle + sliceAngle);
            ctx.arc(cx, cy, innerRadius, startAngle + sliceAngle, startAngle, true);
            ctx.closePath();
            ctx.fillStyle = slice.color;
            ctx.fill();
            startAngle += sliceAngle;
        });

        // 중앙 라벨
        ctx.fillStyle = '#94A3B8';
        ctx.font = '10px Inter, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('총자산', cx, cy - 4);
        ctx.fillStyle = '#F8FAFC';
        ctx.font = 'bold 12px Inter, sans-serif';
        ctx.fillText(CurrencyFormatter.formatKoreanWon(s.currentTotalAssets, true), cx, cy + 12);
    },

    drawDistributionChart() {
        const canvas = document.getElementById('canvas-distribution');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * (window.devicePixelRatio || 1);
        canvas.height = 180 * (window.devicePixelRatio || 1);
        ctx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);

        const width = rect.width > 0 ? rect.width : (canvas.parentElement ? canvas.parentElement.clientWidth : 340);
        const height = 180;
        ctx.clearRect(0, 0, width, height);

        const padding = { top: 20, right: 15, bottom: 30, left: 35 };
        const chartW = width - padding.left - padding.right;
        const chartH = height - padding.top - padding.bottom;

        const mu = this.state.profile.policySettings.wealthDistributionMean;
        const sigma = this.state.profile.policySettings.wealthDistributionStdDev;

        const maxEok = 15.0; // 0 ~ 15억원 표시
        const points = [];
        let maxPdf = 0;

        for (let x = 0.1; x <= maxEok; x += 0.1) {
            const y = LogNormalDistribution.pdf(x, mu, sigma);
            points.push({ x, y });
            if (y > maxPdf) maxPdf = y;
        }

        const getX = (eok) => padding.left + (eok / maxEok) * chartW;
        const getY = (pdf) => padding.top + chartH - (pdf / maxPdf) * chartH;

        // 곡선 채우기 & 선
        ctx.beginPath();
        points.forEach((pt, i) => {
            const px = getX(pt.x);
            const py = getY(pt.y);
            if (i === 0) ctx.moveTo(px, py);
            else ctx.lineTo(px, py);
        });
        ctx.lineTo(getX(maxEok), padding.top + chartH);
        ctx.lineTo(getX(0.1), padding.top + chartH);
        ctx.closePath();
        ctx.fillStyle = 'rgba(59, 130, 246, 0.15)';
        ctx.fill();

        ctx.strokeStyle = '#3B82F6';
        ctx.lineWidth = 2.5;
        ctx.beginPath();
        points.forEach((pt, i) => {
            const px = getX(pt.x);
            const py = getY(pt.y);
            if (i === 0) ctx.moveTo(px, py);
            else ctx.lineTo(px, py);
        });
        ctx.stroke();

        // 대한민국 중위값 (2.4억원) & 평균값 (4.7억원)
        const drawRefLine = (eok, label, color) => {
            const x = getX(eok);
            ctx.strokeStyle = color;
            ctx.setLineDash([3, 3]);
            ctx.beginPath();
            ctx.moveTo(x, padding.top + 10);
            ctx.lineTo(x, padding.top + chartH);
            ctx.stroke();
            ctx.setLineDash([]);
            ctx.fillStyle = color;
            ctx.font = '10px Inter, sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText(label, x, padding.top + 6);
        };

        drawRefLine(2.4, '중위 2.4억', '#94A3B8');
        drawRefLine(4.7, '평균 4.7억', '#F59E0B');

        // 내 순자산 마커 핀
        const curEok = Math.min(maxEok, this.state.summary.currentNetWorth / 100000000.0);
        const myX = getX(curEok);
        const myPdf = LogNormalDistribution.pdf(curEok, mu, sigma);
        const myY = getY(myPdf);

        ctx.strokeStyle = '#10B981';
        ctx.lineWidth = 2.5;
        ctx.beginPath();
        ctx.moveTo(myX, padding.top);
        ctx.lineTo(myX, padding.top + chartH);
        ctx.stroke();

        ctx.fillStyle = '#10B981';
        ctx.beginPath();
        ctx.arc(myX, myY, 6, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = '#10B981';
        ctx.font = 'bold 11px Inter, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('내 위치', myX, padding.top - 4);

        // X축 라벨
        ctx.fillStyle = 'rgba(148, 163, 184, 0.8)';
        ctx.font = '10px Inter, sans-serif';
        ctx.textAlign = 'center';
        [0, 2.5, 5, 7.5, 10, 12.5, 15].forEach(e => {
            ctx.fillText(`${e}억`, getX(e), padding.top + chartH + 16);
        });
    },

    setupEventListeners() {
        // 창 리사이즈 시 차트 다시 그리기
        window.addEventListener('resize', () => {
            if (this.state.currentTab === 'DASHBOARD') {
                this.initDashboardCharts();
            }
        });
    }
};

window.AppSimulator = AppSimulator;
