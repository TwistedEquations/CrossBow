package com.crossbow.wear.core;

import com.android.volley.ParseError;

/**
 * Used to change/shrink the response on the phone to make it easier for the wearable to handle. The ImageRequestTransformer is a good example.
 * All responses are gzipped to save on bluetooth bandwidth so using a ResponseTransformer to zid the response would be counter productive
 */
public interface ResponseTransformer {

    /**
     * Parse and compress the data before the wearable sees it. The returns data here will be the data that the wearable has to
     * parse.
     * @param requestArgs a bundle of args gotten from {@link WearRequest#getTransformerParams()}
     * @param data the data from the network.
     * @return the shrunk data for the wearable request to parse
     */
    byte[] transform(ParamsBundle requestArgs, byte[] data) throws ParseError;
}
