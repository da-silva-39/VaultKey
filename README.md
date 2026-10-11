# VaultKey — Secure Password & Authenticator

Aplicação Android nativa (Java + XML) que combina um **gestor de credenciais**
com um **autenticador 2FA (TOTP/HOTP)**, a funcionar **100% offline**, sem contas,
sem servidores e sem nuvem.

## Objectivos

- Cofre digital local com PIN, cifragem real e bloqueio automático.
- CRUD completo de credenciais com pesquisa, categorias e favoritos.
- Gerador de senhas fortes (SecureRandom) com indicador de força.
- Authenticator TOTP (RFC 6238) e HOTP (RFC 4226), com cadastro por QR Code
  (`otpauth://`) ou manual.
- Arquitectura profissional (Clean + MVVM + Repository), testável e documentada.

## Funcionalidades

| Área | Estado |
|---|---|
| Splash / criação do cofre / PIN | ✅ |
| Desbloqueio com bloqueio progressivo | ✅ |
| Biometria (framework, API 28+) | ✅ |
| Dashboard (contagens, favoritos, pesquisa, atalhos) | ✅ |
| Credenciais: criar, ver (oculta), editar, eliminar, pesquisar, filtrar, favoritar, copiar, abrir URL | ✅ |
| Gerador de senhas + força + cópia temporária | ✅ |
| TOTP com contagem decrescente e actualização automática | ✅ |
| HOTP com contador persistente e avanço transaccional | ✅ |
| QR Code offline (câmara + ZXing) e cadastro manual validado | ✅ |
| Clipboard com limpeza automática configurável | ✅ |
| Bloqueio automático / ao sair / imediato, `FLAG_SECURE` nos ecrãs sensíveis | ✅ |
| Ajustes persistentes, eliminação permanente do cofre com confirmação | ✅ |

## Tecnologias

- Android Studio, **Java 11**, XML, AndroidX (`appcompat`, `material`, `recyclerview`).
- `ViewModel` + `LiveData` (lifecycle 2.9.4), Fragments com navegação manual.
- **Room 2.8.5** (2.x, sobre SQLite) com `annotationProcessor` em Java.
- Android Keystore + **AES-GCM-256** + `SecureRandom` + **PBKDF2-HMAC-SHA256** (210 000 iterações).
- `android.hardware.biometrics.BiometricPrompt` (API 28+), sem dependências novas.
- QR Code: API Camera do framework + **ZXing core 3.5.3** (descodificação local).
- JUnit 4 (unitários), Espresso/Room em memória (instrumentados).

> **Decisões de compatibilidade.** `minSdk 26` (Android 8.0): cobre Keystore
> AES/GCM (23+), `java.util.Base64` (26+) e BiometricPrompt nativo (28+, com
> fallback para PIN em 26/27); `targetSdk/compileSdk 37`. Sem Hilt e sem
> Navigation Component: DI manual explícita (`di.ServiceLocator`, âmbito de
> aplicação) e transacções de Fragments — menos risco, ciclo de vida claro.
> Sem `androidx.biometric`/CameraX: indisponíveis offline; usam-se as APIs
> nativas equivalentes. Sem permissão de INTERNET.

## Arquitectura

```
presentation/   Activities, Fragments, ViewModels, Adapters (sem SQL, sem cripto)
domain/         modelos, contratos de repositório, casos de uso, validações (Java puro)
data/           Room (entidades, DAOs, VaultDatabase), repositórios, mapeadores
security/       Keystore, AES-GCM, PIN/PBKDF2, sessão, biometria, clipboard
authenticator/  Base32, HOTP, TOTP, URI otpauth:// (Java puro, testável)
di/             ServiceLocator (composição manual)
common/         Result<T>, AppExecutors
```

Fronteira de confiança: o DAO nunca vê texto simples sensível; a cifragem vive
nos repositórios (`CredentialRepositoryImpl`, `AuthenticatorRepositoryImpl`)
com AAD `âmbito:id:campo`; a sessão (`SessionManager`) é verificada antes de
cada operação sensível (check-on-access, sem temporizadores frágeis).

## Estrutura de pastas

```
app/src/main/java/mz/ac/ucm/feng/ti/vaultkey/
├── VaultKeyApplication.java
├── presentation/{splash,vault,dashboard,credentials,passwordgenerator,authenticator,settings}
├── domain/{model,repository,usecase,validation,password}
├── data/{local/{dao,entity},repository,mapper}
├── security/{crypto,keystore,pin,session,biometric,clipboard}
├── authenticator/{base32,hotp,totp,uri}
├── di/ServiceLocator.java
└── common/
app/src/main/res/{layout,drawable,menu,values,xml}
app/src/test/          # unitários (JVM)
app/src/androidTest/   # instrumentados (Room em memória)
```

## Como abrir e executar

1. Abrir a pasta do projecto no Android Studio (Ladybug ou superior).
2. Aguardar a sincronização do Gradle (é necessário acesso a `google()` e
   `mavenCentral()` na primeira vez).
3. Executar em emulador ou dispositivo com **API 26+**:
   `Run > Run 'app'` (ou `./gradlew :app:installDebug`).
4. Na primeira abertura: criar o cofre e definir o PIN (4–12 dígitos).

## Testes

```bash
./gradlew :app:testDebugUnitTest          # 28 testes unitários (JVM)
./gradlew :app:connectedDebugAndroidTest  # instrumentados (precisa de emulador)
```

Cobertura unitária: Base32 (RFC 4648), HOTP (**vectores RFC 4226**),
TOTP (**vectores RFC 6238** SHA-1/256/512), URIs `otpauth://`, PIN/PBKDF2,
AES-GCM (round-trip, nonces únicos, detecção de adulteração, AAD errado,
chave errada), gerador/força de senhas e validações de negócio.
Instrumentados: CRUD Room, pesquisa, favoritos e contador HOTP transaccional.

**Resultado actual: 28/28 unitários a passar; `assembleDebug` e compilação dos
testes instrumentados com sucesso.**

## Segurança (resumo)

- Senhas, notas e segredos 2FA guardados **sempre cifrados** (AES-GCM, nonce de
  96 bits único, AAD por registo); falhas de integridade rejeitam os dados.
- Chave AES-256 no **Android Keystore** (nunca no código/recursos/BD); o PIN é
  só autenticador (verificador PBKDF2 com salt) — nunca chave directa.
- Biometria autoriza uma **chave de encapsulamento** do Keystore
  (`setUserAuthenticationRequired`); invalidação do sistema → exige PIN.
- Sem segredos em logs; backup do Android exclui a BD e o ficheiro seguro;
  `FLAG_SECURE` nos ecrãs sensíveis; clipboard limpo apenas se inalterado.
- **Sem o PIN e sem as chaves do dispositivo, os dados são irrecuperáveis.**
  Não existe recuperação online. A eliminação do cofre apaga dados, metadados,
  chaves e token biométrico (eliminação lógica — sem garantia de sobrescrita
  física do armazenamento).

## Limitações conhecidas

- Sem sincronização/backup em nuvem (por desenho); trocar de dispositivo exige
  recadastro manual.
- TOTP depende do relógio do dispositivo; HOTP exige contadores sincronizados.
- Leitor QR usa a API Camera clássica (migração para Camera2 prevista).
- Protecção em memória limitada pelo Android/Java; dispositivo com root ou
  comprometido não pode ser totalmente defendido.
- Biometria indisponível em API < 28 (fallback para PIN).
