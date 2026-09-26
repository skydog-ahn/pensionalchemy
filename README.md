# 🌟 연금술사 (PensionAlchemy) v1.2.5

> **대한민국 100세 생애 은퇴·연금 시뮬레이터 & 종합 재무 설계 솔루션**  
> 3층 연금 피라미드 구축, 소득 크레바스 방어, 4% 안전 인출 룰, 6대 전문 금융 계산기, 대한민국 순자산 백분위 분석

---

## 🌐 온라인 사용설명서 & 인터랙티브 웹 시뮬레이터

별도의 앱 설치 없이 브라우저에서 실제 안드로이드 앱과 100% 동일한 화면과 계산 엔진을 체험할 수 있는 공식 웹 매뉴얼을 제공합니다.

👉 **웹 시뮬레이터 바로가기**: [https://skydog-ahn.github.io/pensionalchemy/](https://skydog-ahn.github.io/pensionalchemy/)  
*(GitHub Pages의 `/docs` 브랜치 설정을 통해 서비스됩니다.)*

---

## 📱 프로젝트 구조 (Repository Architecture)

이 저장소는 **Android 모바일 앱 소스 코드**와 **GitHub Pages 공식 웹 매뉴얼**이 하나로 통합된 리포지토리입니다.

```text
pensionalchemy/
├── app/                     # Android Jetpack Compose 네이티브 앱 모듈
│   ├── src/                 # Kotlin 소스 코드 (UI, ViewModel, Engine, Data)
│   └── build.gradle.kts     # 앱 모듈 빌드 설정
├── docs/                    # 📖 GitHub Pages 공식 웹 매뉴얼 & 시뮬레이터
│   ├── index.html           # 2열 반응형 매뉴얼 & 스마트폰 프레임 웹앱
│   ├── css/style.css        # Material 3 글래스모피즘 & 다크/라이트 테마
│   ├── js/                  # 시뮬레이터 엔진, 차트 렌더러, 재무 계산 엔진
│   │   ├── engines/         # 시뮬레이션, 세법, 대출, 건강도, 계산기 모듈
│   │   ├── simulator.js     # 가상 스마트폰 UI 인터랙션 컨트롤러
│   │   ├── models.js        # 데이터 모델 및 20~60대 생애주기 프리셋
│   │   └── manual.js        # 실시간 매뉴얼 검색, 목차 내비게이션
│   └── .nojekyll            # GitHub Pages 정적 배포 최적화 설정
├── data/                    # 통계청 2025 가계금융복지조사 순자산 데이터셋
├── images/                  # 앱 스크린샷 및 브랜딩 에셋
├── gradle/                  # Gradle 래퍼 파일
├── build.gradle.kts         # 루트 Gradle 빌드 스크립트
├── settings.gradle.kts      # Gradle 프로젝트 설정
└── README.md                # 프로젝트 안내 문서
```

---

## ✨ 핵심 5대 설계 철학 & 기능

1. **🏛️ 3층 연금 피라미드 안전망 구축**
   - 1층 국민연금(물가연동 종신보장), 2층 퇴직연금(10년 이상 분할인출 절세), 3층 개인연금저축/보험, 그리고 보유 주택을 역모기지화하는 주택연금의 황금비율 결합.
2. **⚠️ 소득 크레바스(공백기) 자동 진단**
   - 법정 정년(60세)부터 국민연금 수령 개시(63~65세) 사이 소득이 단절되는 위험 구간을 자동 감지하고 필요 브릿지 자금 역산.
3. **📊 대한민국 순자산 백분위 분석 (로그정규분포)**
   - 2025년 가계금융복지조사 데이터 기반($\mu=1.00984, \sigma=1.1937$), 평균(4.7억)과 중위(2.4억)의 기만을 극복하고 내 순자산의 실제 대한민국 상위 백분위를 시각화.
4. **🧮 6대 전문 재무 계산기**
   - ① 적립·예탁 미래가치 (복리)
   - ② 자산 인출 시뮬레이션 (정액 vs 정률)
   - ③ 자산 고갈 타이머
   - ④ 은퇴 목표 필요자산 역산
   - ⑤ 국민연금 조기(-30%) vs 정상 vs 연기(+36%) 손익분기 연령
   - ⑥ 연금저축/IRP 900만원 세액공제 및 과세이연 절세 연금술
5. **🛡️ 100세 생애 자산 궤적 & 은퇴 건강도 점수**
   - 100세까지의 순자산, 총자산, 부채 상환 추이를 4개 캔버스 차트로 정밀 시뮬레이션하고 0~100점(S/A/B/C/D) 건강 등급 산출.

---

## 🛠️ 안드로이드 앱 빌드 및 실행

### 개발 환경
- **IDE**: Android Studio Ladybug / Koala 이상
- **Kotlin**: 2.0+
- **UI Framework**: Jetpack Compose (Material 3)
- **Min SDK**: 26 (Android 8.0) / **Target SDK**: 35 (Android 15)

### 빌드 명령어
```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 단위 테스트 실행
./gradlew test
```

---

## 📖 GitHub Pages 배포 설정 안내 (Repository 관리자용)

웹 매뉴얼을 정상 서비스하려면 GitHub 저장소에서 다음 설정을 확인하세요:
1. 저장소 상단 **Settings** → 좌측 **Pages** 메뉴 이동
2. **Build and deployment** > **Source**: `Deploy from a branch` 선택
3. **Branch**: `main`, 폴더: **`/docs`** 선택 후 **Save** 클릭
4. 배포 완료 후 `https://<username>.github.io/<repository>/` 에서 라이브 서비스 시작
