import { View, StatusBar, Modal } from "react-native";
import { WebView } from "react-native-webview";
import { useState, useRef } from "react";
import * as Notifications from "expo-notifications";
import { CameraView, useCameraPermissions } from "expo-camera";

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

type Page = "login" | "home" | "intake" | "schedule" | "refill" | "setup" | "settings"| "dashboard" | "adherence-detail" | "taken-missed-detail" | "vitamins-detail" | "weekly-detail" | "recent-detail";

export default function HomeScreen() {
    const [page, setPage] = useState<Page>("login");
    const [showCamera, setShowCamera] = useState(false);
    const [permission, requestPermission] = useCameraPermissions();

    const webViewRef = useRef<any>(null);
    const authTokenRef = useRef<string>("");
    const apiBaseRef = useRef<string>("");
    const usernameRef = useRef<string>("");
    const scannedRef = useRef(false);

    const lastScheduleRef = useRef<any>(null);

    const sources = {
        login: require("../../assets/html/login.html"),
        home: require("../../assets/html/home.html"),
        intake: require("../../assets/html/intake.html"),
        schedule: require("../../assets/html/schedule.html"),
        refill: require("../../assets/html/refill.html"),
        setup: require("../../assets/html/setup.html"),
        settings: require("../../assets/html/settings.html"),
        dashboard: require("../../assets/html/dashboard/dashboard.html"),
        "adherence-detail": require("../../assets/html/dashboard/adherence-detail.html"),
        "taken-missed-detail": require("../../assets/html/dashboard/taken-missed-detail.html"),
        "vitamins-detail": require("../../assets/html/dashboard/vitamins-detail.html"),
        "weekly-detail": require("../../assets/html/dashboard/weekly-detail.html"),
        "recent-detail": require("../../assets/html/dashboard/recent-detail.html"),
    };

    const injectedJS = `
        document.addEventListener('click', function(e) {
            var link = e.target.closest('a');
            if (!link) return;
            var href = link.getAttribute('href');
            if (!href) return;

            if (href.endsWith('login.html')) {
                e.preventDefault();
                window.ReactNativeWebView.postMessage('login');
                return;
            }

            if (href.endsWith('home.html')) {
                e.preventDefault();
                window.ReactNativeWebView.postMessage('home');
                return;
            }

            if (href.endsWith('intake.html')) {
                e.preventDefault();
                window.ReactNativeWebView.postMessage('intake');
                return;
            }

            if (href.endsWith('schedule.html')) {
                e.preventDefault();
                window.ReactNativeWebView.postMessage('schedule');
                return;
            }

            if (href.endsWith('refill.html')) {
                e.preventDefault();
                window.ReactNativeWebView.postMessage('refill');
                return;
            }
            
            if (href.endsWith('setup.html')) {
                e.preventDefault();
                window.ReactNativeWebView.postMessage('setup');
                return;
            }
            
            if (href.endsWith('settings.html')) {
                e.preventDefault();
                window.ReactNativeWebView.postMessage('settings');
                return;
            }
            
            if (href.endsWith('dashboard.html')) { e.preventDefault(); window.ReactNativeWebView.postMessage('dashboard'); }
            if (href.endsWith('adherence-detail.html')) { e.preventDefault(); window.ReactNativeWebView.postMessage('adherence-detail'); }
            if (href.endsWith('taken-missed-detail.html')) { e.preventDefault(); window.ReactNativeWebView.postMessage('taken-missed-detail'); }
            if (href.endsWith('vitamins-detail.html')) { e.preventDefault(); window.ReactNativeWebView.postMessage('vitamins-detail'); }
            if (href.endsWith('weekly-detail.html')) { e.preventDefault(); window.ReactNativeWebView.postMessage('weekly-detail'); }
            if (href.endsWith('recent-detail.html')) { e.preventDefault(); window.ReactNativeWebView.postMessage('recent-detail'); }
        }, true);
        true;
    `;

    const handleBarcodeScanned = async ({ data }: { data: string }) => {
        if (scannedRef.current) return;
        scannedRef.current = true;
        setShowCamera(false);

        try {
            const res = await fetch(
                `${apiBaseRef.current}/api/mobile/barcode/${data}`,
                { headers: { Authorization: `Bearer ${authTokenRef.current}` } }
            );
            const json = await res.json();
            const result = res.ok ? json : null;

            webViewRef.current?.injectJavaScript(
                `window.onBarcodeResult(${JSON.stringify(result)}); true;`
            );
        } catch (e) {
            webViewRef.current?.injectJavaScript(
                `window.onBarcodeResult(null); true;`
            );
        }
    };

    return (
        <View style={{ flex: 1, backgroundColor: "#F5F0E8" }}>
            <StatusBar
                barStyle="light-content"
                backgroundColor="transparent"
                translucent
            />

            <WebView
                ref={webViewRef}
                originWhitelist={["*"]}
                source={sources[page]}
                style={{ flex: 1 }}
                injectedJavaScript={injectedJS}

                onLoad={() => {
                    if (authTokenRef.current && usernameRef.current) {
                        webViewRef.current?.injectJavaScript(`
                            sessionStorage.setItem("authToken", ${JSON.stringify(authTokenRef.current)});
                            sessionStorage.setItem("authUser", ${JSON.stringify(usernameRef.current)});
                            sessionStorage.setItem("apiBase", ${JSON.stringify(apiBaseRef.current)});
                            if (typeof fetchPauseState === "function") fetchPauseState();
                            true;
                        `);
                    }
                }}

                onMessage={async (event) => {
                    const msg = event.nativeEvent.data;

                    try {
                        const parsed = JSON.parse(msg);

                        if (parsed.type === "authSuccess") {
                            authTokenRef.current = parsed.token;
                            apiBaseRef.current = parsed.apiBase;
                            usernameRef.current = parsed.username;

                            // Check if user already has a device claimed
                            try {
                                const res = await fetch(`${parsed.apiBase}/api/mobile/has-device`, {
                                    headers: { Authorization: `Bearer ${parsed.token}` }
                                });
                                if (res.ok) {
                                    const data = await res.json();
                                    setPage(data.hasDevice ? "home" : "setup");
                                } else {
                                    setPage("setup"); // fail safe — show setup if check fails
                                }
                            } catch (e) {
                                setPage("setup"); // fail safe — show setup if network error
                            }
                            return;
                        }

                        if (parsed.type === "pauseToggled") {
                            if (parsed.paused) {
                                await Notifications.cancelAllScheduledNotificationsAsync();
                            } else {
                                if (lastScheduleRef.current) {
                                    await scheduleAllNotifications(
                                        lastScheduleRef.current,
                                        authTokenRef.current,
                                        apiBaseRef.current
                                    );
                                }
                            }
                            return;
                        }

                        if (parsed.type === "scheduleUpdated") {
                            lastScheduleRef.current = parsed.schedule;

                            await scheduleAllNotifications(
                                parsed.schedule,
                                parsed.authToken,
                                parsed.apiBase
                            );
                            return;
                        }

                        if (parsed.type === "scanBarcode") {
                            authTokenRef.current = parsed.authToken;
                            apiBaseRef.current = parsed.apiBase;
                            scannedRef.current = false;

                            if (!permission?.granted) {
                                await requestPermission();
                            }

                            setShowCamera(true);
                            return;
                        }

                    } catch (_) {}

                    if (msg === "home") {
                        setPage("home");
                        setTimeout(() => {
                            webViewRef.current?.injectJavaScript(`
                            sessionStorage.removeItem("setupSource");
                            true;
                            `);
                        }, 50);
                    }
                    if (msg === "login") setPage("login");
                    if (msg === "intake") setPage("intake");
                    if (msg === "schedule") setPage("schedule");
                    if (msg === "refill") setPage("refill");
                    if (msg === "setup_from_settings") {
                        setPage("setup");
                        webViewRef.current?.injectJavaScript(`
                        sessionStorage.setItem("setupSource", "settings");
                        true;
                        `);
                        return;
                    }

                    if (msg === "setup") {
                        setPage("setup");
                        webViewRef.current?.injectJavaScript(`
                        sessionStorage.setItem("setupSource", "onboarding");
                        true;
                        `);
                        return;
                    }
                    if (msg === "settings") {
                        setPage("settings");
                        setTimeout(() => {
                            webViewRef.current?.injectJavaScript(`
                            sessionStorage.removeItem("setupSource");
                            true;
                            `);
                        }, 50);
                    }
                    if (msg === "dashboard") setPage("dashboard");
                    if (msg === "adherence-detail") setPage("adherence-detail");
                    if (msg === "taken-missed-detail") setPage("taken-missed-detail");
                    if (msg === "vitamins-detail") setPage("vitamins-detail");
                    if (msg === "weekly-detail") setPage("weekly-detail");
                    if (msg === "recent-detail") setPage("recent-detail");
                }}
            />

            <Modal visible={showCamera} animationType="slide">
                <View style={{ flex: 1, backgroundColor: "black" }}>
                    {permission?.granted && (
                        <CameraView
                            style={{ flex: 1 }}
                            facing="back"
                            onBarcodeScanned={handleBarcodeScanned}
                        />
                    )}
                </View>
            </Modal>
        </View>
    );
}