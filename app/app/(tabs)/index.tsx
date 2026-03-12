import { View, StatusBar } from "react-native";
import { WebView } from "react-native-webview";
import { useState } from "react";
import * as Notifications from "expo-notifications";

Notifications.setNotificationHandler({
    handleNotification: async () => ({
        shouldShowAlert: true,
        shouldPlaySound: true,
        shouldSetBadge: false,
        shouldShowBanner: true,
        shouldShowList: true,
    }),
});

const DAY_TO_WEEKDAY: Record<string, number> = {
    sunday: 1, monday: 2, tuesday: 3, wednesday: 4,
    thursday: 5, friday: 6, saturday: 7,
};

async function scheduleAllNotifications(payload: any, authToken: string, apiBase: string) {
    await Notifications.cancelAllScheduledNotificationsAsync();

    const { status } = await Notifications.requestPermissionsAsync();
    if (status !== "granted") {
        console.log("Notification permission denied");
        return;
    }

    // ── Dispense notifications ──
// Group all vitamins by day+time
    const timeSlotMap = new Map<string, string[]>();

    for (const vitamin of payload.vitamins ?? []) {
        for (const entry of vitamin.schedule ?? []) {
            for (const time of entry.times ?? []) {
                const key = `${entry.day?.toLowerCase()}|${time}`;
                const existing = timeSlotMap.get(key) ?? [];
                existing.push(`${vitamin.numberOfPills} x ${vitamin.vitaminType}`);
                timeSlotMap.set(key, existing);
            }
        }
    }

    for (const [key, vitamins] of timeSlotMap.entries()) {
        const [day, time] = key.split("|");
        const weekday = DAY_TO_WEEKDAY[day];
        if (!weekday) continue;

        const [hour, minute] = time.split(":").map(Number);

        await Notifications.scheduleNotificationAsync({
            content: {
                title: "Time for your vitamins 💚",
                body: vitamins.join(", "),
            },
            trigger: {
                type: Notifications.SchedulableTriggerInputTypes.WEEKLY,
                weekday,
                hour,
                minute,
            },
        });
    }

    // ── Refill reminder notifications ──
    try {
        const res = await fetch(`${apiBase}/api/mobile/slots/refill-info`, {
            headers: { Authorization: `Bearer ${authToken}` },
        });

        if (res.ok) {
            const refillInfo = await res.json();
            const refillDateStr = refillInfo.refillDate;

            if (refillDateStr) {
                const refillDate = new Date(refillDateStr);

                const threeDaysBefore = new Date(refillDate);
                threeDaysBefore.setDate(threeDaysBefore.getDate() - 3);
                threeDaysBefore.setHours(9, 0, 0, 0);

                if (threeDaysBefore > new Date()) {
                    await Notifications.scheduleNotificationAsync({
                        content: {
                            title: "Refill reminder 🌱",
                            body: "Your Sláinte dispenser needs refilling in 3 days",
                        },
                        trigger: {
                            type: Notifications.SchedulableTriggerInputTypes.DATE,
                            date: threeDaysBefore,
                        },
                    });
                }

                const dayOf = new Date(refillDate);
                dayOf.setHours(9, 0, 0, 0);

                if (dayOf > new Date()) {
                    await Notifications.scheduleNotificationAsync({
                        content: {
                            title: "Refill needed today 🌱",
                            body: "Your Sláinte dispenser needs refilling today",
                        },
                        trigger: {
                            type: Notifications.SchedulableTriggerInputTypes.DATE,
                            date: dayOf,
                        },
                    });
                }
            }
        }
    } catch (e) {
        console.log("Failed to schedule refill notification:", e);
    }
}

export default function HomeScreen() {
    const [page, setPage] = useState<"login" | "home" | "intake" | "schedule">("login");

    const sources = {
        login: require("../../assets/html/login.html"),
        home: require("../../assets/html/home.html"),
        intake: require("../../assets/html/intake.html"),
        schedule: require("../../assets/html/schedule.html"),
    };

    const injectedJS = `
        document.addEventListener('click', function(e) {
            var link = e.target.closest('a');
            if (!link) return;
            var href = link.getAttribute('href');
            if (!href) return;
            if (href.endsWith('login.html')) { e.preventDefault(); window.ReactNativeWebView.postMessage('login'); }
            if (href.endsWith('home.html')) { e.preventDefault(); window.ReactNativeWebView.postMessage('home'); }
            if (href.endsWith('intake.html')) { e.preventDefault(); window.ReactNativeWebView.postMessage('intake'); }
            if (href.endsWith('schedule.html')) { e.preventDefault(); window.ReactNativeWebView.postMessage('schedule'); }
        }, true);
        true;
    `;

    return (
        <View style={{ flex: 1, backgroundColor: "#F5F0E8" }}>
            <StatusBar barStyle="light-content" backgroundColor="transparent" translucent={true} />
            <WebView
                originWhitelist={["*"]}
                source={sources[page]}
                style={{ flex: 1 }}
                injectedJavaScript={injectedJS}
                onMessage={(event) => {
                    const msg = event.nativeEvent.data;

                    try {
                        const parsed = JSON.parse(msg);
                        if (parsed.type === "scheduleUpdated") {
                            scheduleAllNotifications(
                                parsed.schedule,
                                parsed.authToken,
                                parsed.apiBase
                            );
                            return;
                        }
                    } catch (_) {}

                    if (msg === "login") setPage("login");
                    if (msg === "home") setPage("home");
                    if (msg === "intake") setPage("intake");
                    if (msg === "schedule") setPage("schedule");
                }}
            />
        </View>
    );
}