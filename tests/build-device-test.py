from pathlib import Path
import subprocess,os,zipfile
import runpy
config = runpy.run_path(str(Path(__file__).with_name('run-jvm.py')))
r, compiler, libs, sources = (config[k] for k in ['r', 'compiler', 'libs', 'sources'])
sources = [p for p in sources if '/src/test/' not in str(p)]
out=r/'build/device';out.mkdir(parents=True,exist_ok=True)
sources += list((r/'android/src/deviceTest/kotlin').rglob('*.kt'))
subprocess.run(['java','-Xmx3g','-cp',':'.join(compiler),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',':'.join(libs),'-d',str(out/'classes'),*map(str,sources)],check=True)
tools=Path.home()/'Android/Sdk/build-tools/36.1.0'
with zipfile.ZipFile(out/'classes.jar','w') as z:
 for p in (out/'classes').rglob('*.class'): z.write(p,p.relative_to(out/'classes'))
(out/'dex').mkdir(exist_ok=True)
subprocess.run([str(tools/'d8'),'--min-api','26','--lib',libs[-1],'--output',str(out/'dex'),str(out/'classes.jar'),libs[0]],check=True)
(out/'AndroidManifest.xml').write_text('''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="dev.pam.sharetest"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35"/><application android:label="Share N6 Test" android:debuggable="true"><activity android:name="dev.pam.shareextension.ShareDeviceTestActivity" android:exported="true"/><provider android:name="dev.pam.shareextension.ShareDeviceTestProvider" android:authorities="dev.pam.sharetest.files" android:exported="false"/></application></manifest>''')
subprocess.run([str(tools/'aapt2'),'link','-I',libs[-1],'--manifest',str(out/'AndroidManifest.xml'),'-o',str(out/'unsigned.apk')],check=True)
with zipfile.ZipFile(out/'unsigned.apk','a') as z: z.write(out/'dex/classes.dex','classes.dex')
subprocess.run([str(tools/'zipalign'),'-f','4',str(out/'unsigned.apk'),str(out/'test.apk')],check=True)
if not (out/'test.keystore').exists():
 subprocess.run(['keytool','-genkeypair','-keystore',str(out/'test.keystore'),'-storepass','android','-keypass','android','-alias','test','-keyalg','RSA','-dname','CN=N6 Test','-validity','2'],check=True,stdout=subprocess.DEVNULL)
subprocess.run([str(tools/'apksigner'),'sign','--ks',str(out/'test.keystore'),'--ks-pass','pass:android',str(out/'test.apk')],check=True)
print(out/'test.apk')
