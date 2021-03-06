package com.kloudtek.anypoint.util;

import java.io.IOException;
import java.io.InputStream;

public interface StreamSource {
    String getFileName();

    InputStream createInputStream() throws IOException;
}
