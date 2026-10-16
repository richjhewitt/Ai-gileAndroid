B = "app/src/main/java/com/smizflark/aigile/messenger/"

def edit(p, f):
    s = open(p, newline='').read().replace('\r\n', '\n')
    s = f(s)
    open(p, 'w', newline='').write(s.replace('\n', '\r\n'))

def rep(s, a, b):
    assert a in s, a
    return s.replace(a, b, 1)

edit(B + "ui/Lock.kt", lambda s: s)
edit("app/src/test/java/com/smizflark/aigile/messenger/LockTest.kt", lambda s: s)
edit("app/build.gradle.kts", lambda s: rep(s, '    implementation("androidx.work:',
    '    implementation("androidx.biometric:biometric:1.1.0")   // AII-63: lock with fingerprint\n    implementation("androidx.work:'))
edit(B + "data/Store.kt", lambda s: rep(s, '''        set(v) = prefs.edit().putBoolean("alerts", v).apply()
''', '''        set(v) = prefs.edit().putBoolean("alerts", v).apply()

    /** Ask for fingerprint, face or PIN when the app opens (AII-63). */
    var lock: Boolean
        get() = prefs.getBoolean("lock", false)
        set(v) = prefs.edit().putBoolean("lock", v).apply()
'''))

def sett(s):
    s = rep(s, '    var quietOn by remember', '    var lockOn by remember { mutableStateOf(s.lock) }\n    var quietOn by remember')
    s = rep(s, '''            HorizontalDivider()

            Text("Alerts",''', '''            HorizontalDivider()

            Text("Privacy", style = MaterialTheme.typography.titleMedium)
            Toggle("Lock with fingerprint", "Asks for your fingerprint, face or phone PIN when you open the app", lockOn) {
                if (it && !Lock.available(ctx)) note = "This phone has no fingerprint, face or PIN set up yet."
                else { lockOn = it; s.lock = it }
            }
            HorizontalDivider()

            Text("Alerts",''')
    return s
edit(B + "ui/SettingsScreen.kt", sett)

def main(s):
    s = rep(s, 'import androidx.activity.ComponentActivity\n', '')
    s = rep(s, 'import androidx.navigation.NavHostController', 'import androidx.fragment.app.FragmentActivity\nimport androidx.navigation.NavHostController')
    s = rep(s, 'import com.smizflark.aigile.messenger.ui.MessengerTheme', 'import com.smizflark.aigile.messenger.ui.Lock\nimport com.smizflark.aigile.messenger.ui.LockedScreen\nimport com.smizflark.aigile.messenger.ui.MessengerTheme')
    s = rep(s, 'import android.os.Bundle\n', 'import android.os.Bundle\nimport android.os.SystemClock\n')
    s = rep(s, 'class MainActivity : ComponentActivity() {', 'class MainActivity : FragmentActivity() {')
    s = rep(s, '''    private var openNew by mutableStateOf(false)
''', '''    private var openNew by mutableStateOf(false)
    /** App lock (AII-63): covers the app until unlocked; leftAt is when it last went to the background. */
    private var locked by mutableStateOf(false)
    private var unlocked = false
    private var leftAt: Long? = null
''')
    s = rep(s, '''                    openChat = null
                }
''', '''                    openChat = null
                }
                // Covers everything until unlocked; asks again each time it locks.
                if (locked) {
                    LockedScreen(onUnlock = { unlock() })
                    LaunchedEffect(Unit) { unlock() }
                }
''')
    s = rep(s, '''    override fun onStart() { super.onStart(); App.inForeground = true }
    override fun onStop() { super.onStop(); App.inForeground = false }''', '''    private fun unlock() = Lock.ask(this) { unlocked = true; locked = false }

    override fun onStart() {
        super.onStart()
        App.inForeground = true
        if (Lock.shouldLock((application as App).repo.store.lock, unlocked, leftAt, SystemClock.elapsedRealtime())) {
            unlocked = false; locked = true
        }
    }
    override fun onStop() { super.onStop(); App.inForeground = false; leftAt = SystemClock.elapsedRealtime() }''')
    return s
edit(B + "MainActivity.kt", main)
