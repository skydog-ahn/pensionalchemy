/**
 * PensionAlchemy Comprehensive Manual Controller (manual.js)
 * 설명서 사이트의 목차 내비게이션, 검색 필터, 인터랙티브 데모 연동, 테마 토글 등 처리
 */

const ManualController = {
    currentTheme: 'dark',
    activeSection: 'overview',

    init() {
        this.setupTheme();
        this.setupSearch();
        this.setupScrollSpy();
        this.setupPresetSelector();
    },

    setupTheme() {
        const saved = localStorage.getItem('pa_manual_theme') || 'dark';
        this.setTheme(saved);

        const btn = document.getElementById('theme-toggle-btn');
        if (btn) {
            btn.addEventListener('click', () => {
                const nextTheme = this.currentTheme === 'dark' ? 'light' : 'dark';
                this.setTheme(nextTheme);
            });
        }
    },

    setTheme(theme) {
        this.currentTheme = theme;
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('pa_manual_theme', theme);

        const icon = document.getElementById('theme-toggle-icon');
        const text = document.getElementById('theme-toggle-text');
        if (icon) icon.textContent = theme === 'dark' ? '☀️' : '🌙';
        if (text) text.textContent = theme === 'dark' ? '라이트 모드' : '다크 모드';

        // 시뮬레이터 차트 재렌더링
        if (window.AppSimulator && window.AppSimulator.state.currentTab === 'DASHBOARD') {
            window.AppSimulator.initDashboardCharts();
        }
    },

    setupSearch() {
        const input = document.getElementById('manual-search-input');
        if (!input) return;

        input.addEventListener('input', (e) => {
            const query = e.target.value.trim().toLowerCase();
            const cards = document.querySelectorAll('.manual-content-section, .manual-card, .feature-detail-block');

            if (!query) {
                cards.forEach(c => c.style.display = '');
                document.querySelectorAll('.search-no-result').forEach(el => el.remove());
                return;
            }

            let matchCount = 0;
            cards.forEach(c => {
                const text = c.textContent.toLowerCase();
                if (text.includes(query)) {
                    c.style.display = '';
                    matchCount++;
                } else {
                    c.style.display = 'none';
                }
            });

            const existingMsg = document.getElementById('search-result-message');
            if (existingMsg) existingMsg.remove();

            if (matchCount === 0) {
                const mainArea = document.querySelector('.manual-main-article');
                if (mainArea) {
                    const msg = document.createElement('div');
                    msg.id = 'search-result-message';
                    msg.className = 'search-no-result';
                    msg.innerHTML = `🔍 <strong>'${query}'</strong>에 대한 검색 결과가 없습니다. 다른 검색어를 입력해 보세요.`;
                    mainArea.prepend(msg);
                }
            }
        });
    },

    setupScrollSpy() {
        const links = document.querySelectorAll('.sidebar-toc a');
        const sections = document.querySelectorAll('section.manual-section');

        window.addEventListener('scroll', () => {
            let current = '';
            sections.forEach(section => {
                const sectionTop = section.offsetTop - 120;
                if (window.pageYOffset >= sectionTop) {
                    current = section.getAttribute('id');
                }
            });

            links.forEach(link => {
                link.classList.remove('active');
                if (link.getAttribute('href') === `#${current}`) {
                    link.classList.add('active');
                }
            });
        });
    },

    setupPresetSelector() {
        const selector = document.getElementById('global-preset-select');
        if (!selector) return;

        selector.addEventListener('change', (e) => {
            const presetKey = e.target.value;
            if (window.AppSimulator) {
                window.AppSimulator.loadPreset(presetKey);
            }
        });
    },

    onSimulatorUpdated(simState) {
        // 시뮬레이터 변경 시 매뉴얼 내 실시간 동기화 지표 갱신
        const syncElements = document.querySelectorAll('[data-sync]');
        syncElements.forEach(el => {
            const key = el.dataset.sync;
            if (key === 'netWorth') {
                el.textContent = CurrencyFormatter.formatKoreanWon(simState.summary.currentNetWorth);
            } else if (key === 'pensionMonthly') {
                el.textContent = CurrencyFormatter.formatKoreanWon(simState.summary.postRetirementMonthlyPension, true) + '/월';
            } else if (key === 'healthScore') {
                el.textContent = `${simState.summary.healthScore.score}점 (${simState.summary.healthScore.grade})`;
                el.style.color = simState.summary.healthScore.gradeColorHex;
            } else if (key === 'replacementRate') {
                el.textContent = CurrencyFormatter.formatPercent(simState.summary.incomeReplacementRate);
            }
        });
    },

    scrollToSection(id) {
        const el = document.getElementById(id);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    },

    syncSimulatorTab(tabName) {
        if (window.AppSimulator) {
            window.AppSimulator.switchTab(tabName);
            // 모바일 화면 등에서 시뮬레이터로 스크롤 이동
            const simEl = document.querySelector('.phone-device-column');
            if (simEl && window.innerWidth < 1200) {
                simEl.scrollIntoView({ behavior: 'smooth' });
            }
        }
    }
};

window.ManualController = ManualController;
