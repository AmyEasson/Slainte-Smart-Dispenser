import { View, StatusBar } from "react-native";
import { WebView } from "react-native-webview";
import { useState } from "react";

export default function HomeScreen() {
  const [page, setPage] = useState<"login" | "home" | "intake" | "schedule">("login");

  // load local HTML files
  const sources = {
      login: require("../../assets/html/login.html"),
      home: require("../../assets/html/home.html"),
      intake: require("../../assets/html/intake.html"),
      schedule: require("../../assets/html/schedule.html"),
  };

  // intercept <a href="..."> clicks inside HTML
  const injectedJS = `
    document.addEventListener('click', function(e) {
      var link = e.target.closest('a');
      if (!link) return;

      var href = link.getAttribute('href');

      if (!href) return;

      if (href.endsWith('login.html')) {
        e.preventDefault();
        window.ReactNativeWebView.postMessage('login');
      }
      
      if (href.endsWith('home.html')) {
        e.preventDefault();
        window.ReactNativeWebView.postMessage('home');
      }

      if (href.endsWith('intake.html')) {
        e.preventDefault();
        window.ReactNativeWebView.postMessage('intake');
      }

      if (href.endsWith('schedule.html')) {
        e.preventDefault();
        window.ReactNativeWebView.postMessage('schedule');
      }
    }, true);
    true;
  `;

    return (
        <View style={{ flex: 1, backgroundColor: "#F5F0E8" }}>
            <StatusBar
                barStyle="light-content"
                backgroundColor="transparent"
                translucent={true}
            />
            <WebView
                originWhitelist={["*"]}
                source={sources[page]}
                style={{ flex: 1 }}
                injectedJavaScript={injectedJS}
                onMessage={(event) => {
                    const msg = event.nativeEvent.data;

                    if (msg === "login") setPage("login");
                    if (msg === "home") setPage("home");
                    if (msg === "intake") setPage("intake");
                    if (msg === "schedule") setPage("schedule");
                }}
            />
        </View>
    );
}