import {Refine} from "@refinedev/core";
import {DevtoolsPanel, DevtoolsProvider} from "@refinedev/devtools";
import {RefineKbar, RefineKbarProvider} from "@refinedev/kbar";
import {ErrorComponent, useNotificationProvider, RefineSnackbarProvider, ThemedLayoutV2, ThemedTitleV2} from "@refinedev/mui";
import CssBaseline from "@mui/material/CssBaseline";
import GlobalStyles from "@mui/material/GlobalStyles";
import routerBindings, {DocumentTitleHandler, UnsavedChangesNotifier} from "@refinedev/react-router-v6";
import dataProvider from "@refinedev/simple-rest";
import {HashRouter, Navigate, Outlet, Route, Routes} from "react-router-dom";
import {Header} from "./components";
import {ColorModeContextProvider} from "./contexts/color-mode";
import {EventList, SendEventList, ReceivedEventList, EventShow} from "./pages/events";
import type { HttpError } from "@refinedev/core";

function App() {

  return (
    <HashRouter>
      <RefineKbarProvider>
        <ColorModeContextProvider>
          <CssBaseline/>
          <GlobalStyles styles={{html: {WebkitFontSmoothing: "auto"}}}/>
          <RefineSnackbarProvider>
            <DevtoolsProvider>
              <Refine
                dataProvider={dataProvider("/api/eventviewer")}
                notificationProvider={useNotificationProvider}
                routerProvider={routerBindings}
                resources={[
                  {
                    name: "in",
                    list: "/in/",
                    show: "/in/show/:id/",
                    meta: {
                      label: "Received Events",
                      canDelete: false,
                    }
                  },
                  {
                    name: "out",
                    list: "/out/",
                    show: "/out/show/:id/",
                    meta: {
                      label: "Send Events",
                      canDelete: false,
                    }
                  },
                  {
                    name: "failed",
                    list: "/failed/",
                    show: "/failed/show/:id/",
                    meta: {
                      label: "Failed Events",
                      canDelete: false,
                    }
                  }
                ]}
                options={{
                  syncWithLocation: true,
                  warnWhenUnsavedChanges: true,
                  useNewQueryKeys: true,
                  projectId: "Tkk5XX-Islica-4Vk2iq",
                }}
              >
                <Routes>
                  <Route index element={<Navigate to="/in"/>}/>
                  <Route path="/index.html" element={<Navigate to="/in"/>}/>
                  <Route element={
                    <ThemedLayoutV2
                      Header={() => <Header sticky/>}
                      Title={({collapsed}) => (
                        <ThemedTitleV2
                          collapsed={collapsed}
                          text="FEDeRATED"
                        />
                      )}
                    >
                      <Outlet/>
                    </ThemedLayoutV2>
                  }>
                    <Route path="/out">
                      <Route index element={<SendEventList/>}/>
                      <Route path="show/:id" element={<EventShow/>}/>
                    </Route>
                    <Route path="/in">
                      <Route index element={<ReceivedEventList/>}/>
                      <Route path="show/:id" element={<EventShow/>}/>
                    </Route>
                    <Route path="/failed">
                      <Route index element={<EventList/>}/>
                      <Route path="show/:id" element={<EventShow/>}/>
                    </Route>
                    <Route path="*" element={<ErrorComponent/>}/>
                  </Route>
                </Routes>
                <RefineKbar/>
                <UnsavedChangesNotifier/>
                <DocumentTitleHandler/>
              </Refine>
              <DevtoolsPanel/>
            </DevtoolsProvider>
          </RefineSnackbarProvider>
        </ColorModeContextProvider>
      </RefineKbarProvider>
    </HashRouter>
  );
}

export default App;
