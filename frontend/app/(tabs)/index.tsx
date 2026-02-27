import { SafeAreaView } from "react-native";
import { WebView } from "react-native-webview";
import { useState } from "react";

export default function HomeScreen() {
  const [page, setPage] = useState<"index" | "intake" | "schedule">("index");

  // load local HTML files
  const sources = {
    index: require("../../assets/html/index.html"),
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

      if (href.endsWith('index.html')) {
        e.preventDefault();
        window.ReactNativeWebView.postMessage('index');
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
    <SafeAreaView style={{ flex: 1 }}>
      <WebView
        originWhitelist={["*"]}
        source={sources[page]}
        injectedJavaScript={injectedJS}
        onMessage={(event) => {
          const msg = event.nativeEvent.data;

          if (msg === "index") setPage("index");
          if (msg === "intake") setPage("intake");
          if (msg === "schedule") setPage("schedule");
        }}
      />
    </SafeAreaView>
  );
}