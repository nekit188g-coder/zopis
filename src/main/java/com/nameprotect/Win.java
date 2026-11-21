package com.nameprotect;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Виклики Windows API через JNA (JNA вже є в Minecraft). Без PowerShell і без зовнішніх процесів. */
final class Win {
    private Win() {}

    interface U32 extends StdCallLibrary {
        U32 I = Native.load("user32", U32.class);

        boolean EnumWindows(EnumCb cb, Pointer data);

        int GetWindowTextW(Pointer hwnd, char[] buf, int max);

        int GetWindowThreadProcessId(Pointer hwnd, IntByReference pid);

        void keybd_event(byte vk, byte scan, int flags, Pointer extra);
    }

    interface K32 extends StdCallLibrary {
        K32 I = Native.load("kernel32", K32.class);

        Pointer OpenProcess(int access, boolean inherit, int pid);

        boolean QueryFullProcessImageNameW(Pointer h, int flags, char[] buf, IntByReference size);

        boolean CloseHandle(Pointer h);
    }

    interface EnumCb extends StdCallLibrary.StdCallCallback {
        boolean callback(Pointer hwnd, Pointer data);
    }

    private static final Map<Integer, String> EXE = new HashMap<>();
    private static List<String[]> cur;
    /** Статичне посилання, щоб callback не прибрав GC. */
    private static final EnumCb CB = (hwnd, data) -> {
        try {
            char[] buf = new char[512];
            int n = U32.I.GetWindowTextW(hwnd, buf, 512);
            if (n > 0) {
                IntByReference pid = new IntByReference();
                U32.I.GetWindowThreadProcessId(hwnd, pid);
                cur.add(new String[]{exe(pid.getValue()), new String(buf, 0, n)});
            }
        } catch (Throwable ignored) {
        }
        return true;
    };

    private static String exe(int pid) {
        if (pid == 0) return "";
        String c = EXE.get(pid);
        if (c != null) return c;
        String name = "";
        Pointer h = K32.I.OpenProcess(0x1000, false, pid);   // PROCESS_QUERY_LIMITED_INFORMATION
        if (h != null) {
            try {
                char[] b = new char[520];
                IntByReference sz = new IntByReference(520);
                if (K32.I.QueryFullProcessImageNameW(h, 0, b, sz)) {
                    String p = new String(b, 0, sz.getValue());
                    name = p.substring(p.lastIndexOf('\\') + 1).toLowerCase();
                }
            } finally {
                K32.I.CloseHandle(h);
            }
        }
        if (EXE.size() > 600) EXE.clear();
        EXE.put(pid, name);
        return name;
    }

    /** Усі верхньорівневі вікна з непорожнім заголовком: {exe, title}. */
    static synchronized List<String[]> windows() {
        cur = new ArrayList<>();
        U32.I.EnumWindows(CB, null);
        return cur;
    }

    /** Глобальна медіаклавіша: 0xB3 play/pause, 0xB0 next, 0xB1 prev. */
    static void mediaKey(int vk) {
        U32.I.keybd_event((byte) vk, (byte) 0, 0, null);
        U32.I.keybd_event((byte) vk, (byte) 0, 2, null);   // KEYEVENTF_KEYUP
    }
}
