"""Build with the portable JDK 17 and Android SDK under .tools."""
from pathlib import Path
import os, subprocess, zipfile, secrets
root = Path(__file__).resolve().parent
os.chdir(root)
tools = root / ".tools"
jdk = next(tools.glob("jdk-*/bin"))
bt = next(p.parent for p in tools.glob("*/aapt.exe"))
android = next(tools.glob("android-*/android.jar"))
build = root / "build"
classes = build / "classes"
classes.mkdir(parents=True, exist_ok=True)
def run(*args):
    subprocess.run([str(a) for a in args],check=True)
run(bt/"aapt.exe","package","-f","-M","AndroidManifest.xml","-S","res","-A","assets","-I",android,"-F",build/"app.zip")
run(jdk/"javac.exe","--release","8","-encoding","UTF-8","-classpath",android,"-d",classes,*Path("src").glob("*.java"))
jar = build/"code.jar"
run(jdk/"jar.exe","cf",jar,"-C",classes,".")
run(jdk/"java.exe","-cp",bt/"lib/d8.jar","com.android.tools.r8.D8","--min-api","26","--lib",android,"--output",build,jar)
with zipfile.ZipFile(build/"app.zip","a") as apk:
    apk.write(build/"classes.dex","classes.dex")
run(bt/"zipalign.exe","-f","4",build/"app.zip",build/"aligned.apk")
private = root/".signing"
private.mkdir(exist_ok=True)
password = private/"password"
key = private/"ammu.p12"
if not password.exists() and not key.exists():
    password.write_text(secrets.token_urlsafe(32))
os.environ["AMMU_SIGN_PASSWORD"]=password.read_text().strip()
if not key.exists():
    run(jdk/"keytool.exe","-genkeypair","-keystore",key,"-storepass:env","AMMU_SIGN_PASSWORD","-alias","ammu","-keyalg","RSA","-keysize","3072","-validity","10000","-dname","CN=Ammu Reminders")
signer=[jdk/"java.exe","-jar",bt/"lib/apksigner.jar"]
run(*signer,"sign","--ks",key,"--ks-pass","env:AMMU_SIGN_PASSWORD","--out","Ammu-Reminders.apk",build/"aligned.apk")
run(*signer,"verify","--verbose","Ammu-Reminders.apk")

