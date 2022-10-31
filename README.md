## 🚀 프로젝트 내용

---

AWS에서 Naver Cloud Platform으로, Naver Cloud Platform에서 AWS로 인프라 이전을 자동으로 지원하는 도구를 개발합니다.

### 💻 개발환경

- os: Linux Ubuntu(64-bit)
- language: Java

### 💡 프로젝트 필요성

✅ 최근 온-프레미스 환경에서 클라우드 서비스로 인프라를 이전하는 추세입니다.

✅ 때문에 클라우드 간 이전 역시 필요합니다.

<aside>
⚠️ 클라우드 서비스 사용자는 클라우드 서비스의 **이용 약관이 변경**되었거나, 서비스 **제공 업체의 작동이** 일시적 혹은 영구적으로 **중단**되어 사용하지 못할 경우, **어플리케이션 호환성** 문제 발생, 재계약 시 **비용, 성능 측면으로 이득인 다른 플랫폼**이 발견되었을 때 서비스 이전을 시도할 수 있습니다.

</aside>

✅ 위와 같은 이유로 클라우드 서비스 마이그레이션(이전)을 보다 쉽게 하기 위해 마이그레이션 자동화 도구가 필요합니다.

## ⚠️발생문제 및 해결방법

---

## 1️⃣. IaC(Terraform etc.) 사용 불가능

 ✅ IaC는 Infrastructure as Code의 약자로, 인프라를 코드로써 관리할 수 있도록 도움을 줍니다.

      → IaC 도구 Terraform을 활용하면 인프라 이전이 더 쉬워질 것이라고 생각했습니다.

❌ 그러나, Naver Cloud Platform에서 일부 기능이 **Terraform 지원을 하지 않습니다**.

<aside>
💡 따라서 저희는, IaC 도구인 Terraform을 사용하는 대신, **직접 Java로 코딩**하여  Docker image부터 Server까지 이전해보기로 했습니다.

</aside>

## 2️⃣. Server 표현 방식의 다름

‼️ AWS에서의 Server  정보 표현 방식, Naver Cloud Platform의 Server 표현 방식이 다릅니다.

ex) server 이름이 Tag.name, Server.name으로 표기되는 등

<aside>
💡 단순히 AWS, NCP의  Server 표현 방식을 고려하는 것이 아니라, 다른 클라우드 서비스까지의 확장을 고려하여 중간에 Converter 역할을 하는 표준을 세우고 표준에 맞게 변화시킬 수 있도록 하기로 결정했습니다. (ex AWS→Standard→NCP)

</aside>

## 🌎개발 환경

---

- Java

# 💼담당 업무

---

Docker Image들을 옮기고 옮긴 Image를 옮겨진 Server위에 올려 구동시키는 과정까지 담당하고있습니다.

# ✔️현재

---

✅ Docker Image Upload / Download (Java)

✅ 양 클라우드 서비스 서버 정보 변환을 위한 파라미터 조사

✅ 지정한 파라미터로 서버 생성

✅ Server 이전 모듈, Docker image 이전 모듈을 합쳐 AWS to NCP, NCP to AWS 를 완성

✅ 생성된 Server에 Docker image를 pull받아 Run

# 💭앞으로

---

❓ 다른 클라우드로의 확장성 검토

# 📊Block Diagram

---

<img width="466" alt="image" src="https://user-images.githubusercontent.com/26293917/199026383-f4e4bdfc-e415-4223-bd21-d2096320f682.png">


# 📄논문형식 보고서

---

[도커기반 인프라 이전 지원 도구 개발.pdf](https://github.com/eunseo0104/Docker-based-infrastructure-migration-support-tools/files/9901758/default.pdf)
