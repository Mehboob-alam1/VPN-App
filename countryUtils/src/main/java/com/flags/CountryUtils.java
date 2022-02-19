package com.flags;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.Locale;


public class CountryUtils {

   @DrawableRes
   public static int getFlagResIDFromCountryCode(@NonNull Context context, String countryCode) {
      return context.getResources().getIdentifier("ic_flag_" + countryCode, "drawable", context.getPackageName());
   }

   @NonNull
   public static String getLocalizedNameFromCountryCode(String currentLocaleCode, @NonNull String countryCode) {
      if (currentLocaleCode == null || currentLocaleCode.equals("default")) {
         return new Locale("", countryCode).getDisplayCountry(Locale.getDefault());
      } else {
         return new Locale("", countryCode).getDisplayCountry(new Locale(currentLocaleCode));
      }
   }
}
