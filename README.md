# کارخانه هوش مصنوعی یوتیوب (YouTube AI Factory)

اپلیکیشن حرفه‌ای و مدرن بومی اندروید برای تولید، تدوین، سناریونویسی و انتشار خودکار محتوای یوتیوب با رندر ویدیوی محلی MP4 (مبتنی بر MediaCodec و MediaMuxer) و پخش‌کننده Media3 ExoPlayer.

---

## مشخصات فنی پروژه (Technical Specifications)

* **Application ID**: `com.aistudio.ytaifactory.kznqvx`
* **Namespace**: `com.example`
* **Minimum SDK**: `24` (Android 7.0+)
* **Target SDK**: `36` (Android 16)
* **Compile SDK**: `36`
* **Java Version**: JDK 21 (Temurin) / Bytecode Java 11
* **Kotlin Version**: `2.2.10`
* **Android Gradle Plugin (AGP)**: `9.1.1`
* **Gradle Wrapper Version**: `9.3.1`

---

## نحوه ساخت و دریافت APK از طریق GitHub Actions (بدون نیاز به اندروید استودیو)

برای کاربران گوشی‌های اندرویدی و کسانی که از مرورگر موبایل یا Termux استفاده می‌کنند:

### روش ۱: استفاده از مرورگر موبایل در گیت‌هاب (Mobile Browser)

1. مخزن (Repository) خود را در سایت GitHub با مرورگر گوشی باز کنید.
2. به زبانه **Actions** بروید.
3. گردش‌کار **"Build YouTube AI Factory Android App"** را انتخاب کنید.
4. روی دکمه **Run workflow** کلیک کنید تا فرآیند ساخت آغاز شود.
5. پس از پایان ساخت (علامت تیک سبز)، روی عنوان بیلد کلیک کنید و در بخش پایین صفحه (بخش **Artifacts**):
   * فایل **`YouTube-AI-Factory-APK`** را دانلود کنید (حاوی فایل نصبی APK).
   * فایل زیپ را در گوشی اکسترکت کرده و APK را مستقیماً نصب کنید.

### روش ۲: استفاده از خط فرمان یا Termux

```bash
# نصب بسته گیت و ابزارهای لازم در ترموکس
pkg install git gh -y

# کلون مخزن
git clone <URL_REPOS_شما>
cd <نام_پوشه_پروژه>

# اعطای مجوز اجرایی به گرادل
chmod +x gradlew

# تست کامپایل محلی در صورت وجود OpenJDK
./gradlew assembleDebug
```

---

## ساختار ماژول‌ها و کتابخانه‌ها

* **Video Renderer**: انکودینگ فریم‌های تصویری و زیرنویس با `android.media.MediaCodec` به همراه ترکیب استریم صوتی و خروجی استاندارد MP4 با `android.media.MediaMuxer`.
* **Video Player**: پخش‌کننده روان `androidx.media3:media3-exoplayer` با کنترل‌های کامل کاربری.
* **Database**: پایگاه داده محلی پایدار `androidx.room` نسخه ۲ جهت ذخیره پروژه‌ها، سناریوها و وظایف اتوماسیون.
* **UI**: ۱۰۰٪ با **Jetpack Compose** بر پایه Material 3 با تم اختصاصی سرمه‌ای-فیروزه‌ای و پشتیبانی دوطرفه از زبان‌های راست‌به‌چپ (فارسی) و چپ‌به‌راست (انگلیسی).
