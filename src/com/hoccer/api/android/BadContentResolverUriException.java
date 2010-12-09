package com.hoccer.api.android;

public class BadContentResolverUriException extends Exception {

    private static final long serialVersionUID = 1L;

    public BadContentResolverUriException() {
        super();
    }

    public BadContentResolverUriException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BadContentResolverUriException(String detailMessage) {
        super(detailMessage);
    }

    public BadContentResolverUriException(Throwable throwable) {
        super(throwable);
    }

}
