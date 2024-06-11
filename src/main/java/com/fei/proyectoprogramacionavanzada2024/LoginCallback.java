package com.fei.proyectoprogramacionavanzada2024;

import java.io.IOException;
import java.net.Socket;

public interface LoginCallback {
    void onLoginSuccess(Socket socket) throws IOException;
}
