package com.lextezy.tambiolo

object Config {
    /** results.json written nightly by the tambiolo-results GitHub repo. */
    const val RESULTS_URL =
        "https://raw.githubusercontent.com/lextezy123/tambiolo-results/main/data/results.json"

    /** digits.json (Swertres, EZ2, 4D, 6D) from the same repo. */
    const val DIGITS_URL =
        "https://raw.githubusercontent.com/lextezy123/tambiolo-results/main/data/digits.json"

    /** Google's TEST banner id. Replace with your own AdMob banner id before publishing. */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    /** One-time "Tambiolo Plus" product (no ads + full stats). Create a product with this exact id in Play Console. */
    const val PLUS_PRODUCT_ID = "tambiolo_plus"
}
