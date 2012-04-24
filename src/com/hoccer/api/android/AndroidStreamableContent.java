/**
 * Copyright (C) 2010, Hoccer GmbH Berlin, Germany <www.hoccer.com> These coded instructions,
 * statements, and computer programs contain proprietary information of Hoccer GmbH Berlin, and are
 * copy protected by law. They may be used, modified and redistributed under the terms of GNU
 * General Public License referenced below. Alternative licensing without the obligations of the GPL
 * is available upon request. GPL v3 Licensing: This file is part of the "Linccer Android-API".
 * Linccer Android-API is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version. Linccer Android-API is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with Linccer
 * Android-API. If not, see <http://www.gnu.org/licenses/>.
 */
package com.hoccer.api.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ContentResolver;
import android.net.Uri;

import com.hoccer.data.GenericStreamableContent;

public abstract class AndroidStreamableContent extends GenericStreamableContent {

    // Instance Fields ---------------------------------------------------

    /** The content resolver for accessing the data of this content object */
    private ContentResolver mContentResolver;

    /** The URI of this object's content. May be null. */
    private Uri mDataUri;

    // Constructors ------------------------------------------------------

    public AndroidStreamableContent(ContentResolver pContentResolver) {

        mContentResolver = pContentResolver;
    }

    // Public Instance Methods -------------------------------------------

    /** override this in subclass, if you dont set a data URI */
    @Override
    public InputStream openRawInputStream() throws IOException {

        assert null != getDataUri();
        return mContentResolver.openInputStream(getDataUri());
    }

    /** override this in subclass, if you dont set a data URI */
    @Override
    public OutputStream openRawOutputStream() throws IOException {

        assert null != getDataUri();
        return mContentResolver.openOutputStream(getDataUri());
    }

    @Override
    public OutputStream openNewOutputStream() throws IOException {

        return openRawOutputStream();
    }

    @Override
    public InputStream openNewInputStream() throws IOException {

        return openRawInputStream();
    }

    @Override
    public String getContentType() {

        if (getDataUri() != null) {

            return mContentResolver.getType(getDataUri());
        }

        return null;
    }

    /** override this in subclass, if you dont set a data URI */
    @Override
    public long getNewStreamLength() throws IOException {

        assert null != getDataUri();
        return mContentResolver.openAssetFileDescriptor(getDataUri(), "r").getLength();
    }

    /**
     * @return the URI of this object's content
     */
    public Uri getDataUri() {

        return mDataUri;
    }

    // Protected Instance Methods ----------------------------------------

    protected void setDataUri(Uri pContentUri) throws BadContentResolverUriException {

        if (pContentUri == null) {
            throw new BadContentResolverUriException("Content URI is null!");
        }

        mDataUri = pContentUri;
    }

    protected boolean isFileSchemeUri() {

        return "file".equals(mDataUri.getScheme());
    }

    // override this in subclass, if you dont set a contentresolver uri
    @Override
    public long getRawStreamLength() throws IOException {
        if (mDataUri == null) {
            // Log.e(LOG_TAG, "no valid content resolver uri!");
        }

        return mContentResolver.openAssetFileDescriptor(getDataUri(), "r").getLength();
    }

}
