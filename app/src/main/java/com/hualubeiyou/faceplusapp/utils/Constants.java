package com.hualubeiyou.faceplusapp.utils;

/**
 * Created by flight on 2016/12/15
 */

public class Constants {

    public static final String TAG_APPLICATION = "faceplusapp";

    static final boolean TAG_VERSION_DEBUG = true;

    public static final String authority = "com.hualubeiyou.android.fileprovider";

    public static final String FILE_NAME_SUFFIX_FORMAT="yyyyMMdd";

    public static final int UP_LIMIT_FILES = 10;

    public static final String API_KEY_APPLICATION = "cziQie5URhJeCD_MsbsVyRNE0t2bCo6W";

    public static final String API_SECRET_APPLICATION = "KRsi-np3o15OMftmG9kSFy3LiTAYWPiv";

    public static final String OUTER_ID_TEST = "OUTER_ID_DEVELOPMENT_8";

    public static final String OUTER_ID_KEY = "OUTER_ID_KEY";

    public static int DETECT_USE_LIMIT = 5;

    public static int IMAGE_FILE_UPLIMIT = 2 * 1024 * 1024;

    //----------------POST REQUEST----------------------------------------------------------
    // FaceSet Create API
    public static final String URL_FACESET_CREATE = "https://api-cn.faceplusplus.com/facepp/v3/faceset/create";
    public static final String PARAMETER_API_KEY = "api_key";
    public static final String PARAMETER_API_SECRET = "api_secret";
    public static final String PARAMETER_OUTER_ID = "outer_id";

    // Detect API
    public static final String URL_FACE_DETECT = "https://api-cn.faceplusplus.com/facepp/v3/detect";
    public static final String PARAMETER_IMAGE_FILE = "image_file";
    public static final String PARAMETER_RETURN_ATTRIBUTES = "return_attributes";
    public static final String VALUE_RETURN_FACES = "faces";
    public static final String VALUE_RETURN_FACE_TOKEN = "face_token";

    // Face SetUserID API
    public static final String URL_FACE_SETUSERID = "https://api-cn.faceplusplus.com/facepp/v3/face/setuserid";
    public static final String PARAMETER_USER_ID = "user_id";

    // FaceSet AddFace API
    public static final String URL_FACESET_ADDFACE = "https://api-cn.faceplusplus.com/facepp/v3/faceset/addface";
    public static final String PARAMETER_FACE_TOKENS = "face_tokens";
    public static final String VALUE_RETURN_FACE_COUNT = "face_count";
    public static final String VALUE_RETURN_FACE_ADDED = "face_added";

    // Search API
    public static final String URL_FACE_SEARCH = "https://api-cn.faceplusplus.com/facepp/v3/search";
    public static final String VALUE_RETURN_RESULTS = "results";
    public static final String PARAMTER_RESULT_COUNT = "return_result_count";
    public static final String DEFAULT_RESULT_COUNT = "5";

}
