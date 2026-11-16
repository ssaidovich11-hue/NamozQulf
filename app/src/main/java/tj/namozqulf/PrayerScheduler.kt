package tj.namozqulf

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

object PrayerScheduler {
    const val PREFS = "namoz_qulf_preferences"
    private const val ACTION_PRAYER = "tj.namozqulf.PRAYER_REMINDER"
    private const val ACTION_END = "tj.namozqulf.END_FOCUS"
    private val prayerNames = listOf("Бомдод", "Пешин", "Аср", "Шом", "Хуфтан")
    private val defaults = listOf("05:00", "12:30", "16:00", "18:30", "20:00")

    fun scheduleAll(context: Context) {
        prayerNames.indices.forEach { schedulePrayer(context, it) }
    }

    private fun schedulePrayer(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val time = prefs.getString("time_$index", defaults[index]) ?: defaults[index]
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -20)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER
            putExtra("prayer_index", index)
        }
        val pending = PendingIntent.getBroadcast(context, 100 + index, intent, PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag())
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pending)
    }

    fun reschedulePrayer(context: Context, index: Int) = schedulePrayer(context, index)

    fun startFocus(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.isNotificationPolicyAccessGranted) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = manager.currentInterruptionFilter
        prefs.edit().putInt("previous_filter", previous).apply()
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        scheduleFocusEnd(context, prefs.getInt("focus_minutes", 30))
    }

    fun scheduleFocusEnd(context: Context, minutes: Int) {
        val intent = Intent(context, FocusEndReceiver::class.java).setAction(ACTION_END)
        val pending = PendingIntent.getBroadcast(context, 501, intent, PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag())
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, pending)
    }

    fun endFocus(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.isNotificationPolicyAccessGranted) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
            val previous = prefs.getInt("previous_filter", NotificationManager.INTERRUPTION_FILTER_ALL)
            manager.setInterruptionFilter(previous)
        }
    }

    fun notify(context: Context, id: Int, title: String, message: String) {
        createNotificationChannel(context)
        val open = PendingIntent.getActivity(
            context, 900 + id, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
        val notification = NotificationCompat.Builder(context, "prayer_focus")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
    }

    fun immutableFlag(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val index = intent.getIntExtra("prayer_index", -1)
        if (index !in 0..4) return
        val prefs = context.getSharedPreferences(PrayerScheduler.PREFS, Context.MODE_PRIVATE)
        val names = listOf("Бомдод", "Пешин", "Аср", "Шом", "Хуфтан")
        PrayerScheduler.reschedulePrayer(context, index)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.isNotificationPolicyAccessGranted) {
            PrayerScheduler.startFocus(context)
            PrayerScheduler.notify(context, 200 + index, "Вақти намози ${names[index]}", "Режими тамаркуз фаъол шуд.")
        } else {
            PrayerScheduler.notify(context, 200 + index, "20 дақиқа то намози ${names[index]}", "Барои фаъол кардани режими тамаркуз ба барнома иҷозати «Халал нарасонед» диҳед.")
        }
    }
}

class FocusEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerScheduler.endFocus(context)
        PrayerScheduler.notify(context, 700, "Режими тамаркуз анҷом ёфт", "Вақти интихобшуда ба охир расид.")
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") PrayerScheduler.scheduleAll(context)
    }
}
