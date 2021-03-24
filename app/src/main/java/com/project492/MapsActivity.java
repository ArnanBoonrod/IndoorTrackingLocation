package com.project492;

import androidx.fragment.app.FragmentActivity;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

public class MapsActivity extends FragmentActivity implements  OnMapReadyCallback {
    private GoogleMap mMap;
    private static final int MAX_DIMENSION = 2048;
    private IALocationManager mManager;
    private Marker mMapMarker;
    private Target mLoadTarget;
    private IARegion mOverlayFloorPlan = null;
    private GroundOverlay mGroundOverlay = null;
    private boolean update = true;
    public Integer floor;

    private  String TAG = "Indoor Tracking Location";

    private IALocationListener mListener = new IALocationListener() {
        @Override
        public void onLocationChanged(IALocation iaLocation) {
            floor = iaLocation.hasFloorLevel() ? iaLocation.getFloorLevel() : null;
            LatLng latLng = new LatLng(iaLocation.getLatitude(), iaLocation.getLongitude());
            if (mMapMarker == null) {
                try {
                    mMapMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.dot)));
                }
                catch(Exception e) {
                    Log.d(TAG, "location is null");
                }
            } else {
                mMapMarker.setPosition(latLng);
            }
            if (update) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
                update = false;
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }
    };

        private void setupGroundOverlay(IAFloorPlan floorPlan, Bitmap bitmap) {

        if (mGroundOverlay != null) {
            mGroundOverlay.remove();
        }

        if (mMap != null) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            IALatLng iaLatLng = floorPlan.getCenter();
            LatLng center = new LatLng(iaLatLng.latitude, iaLatLng.longitude);
            GroundOverlayOptions fpOverlay = new GroundOverlayOptions()
                    .image(bitmapDescriptor)
                    .position(center, floorPlan.getWidthMeters(), floorPlan.getHeightMeters())
                    .bearing(floorPlan.getBearing());

            mGroundOverlay = mMap.addGroundOverlay(fpOverlay);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mManager = IALocationManager.create(this);
        ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map))
                .getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    public void onResume() {
        super.onResume();
        mManager.lockIndoors(false);
        IALocationRequest locReq = IALocationRequest.create();
        locReq.setPriority(IALocationRequest.PRIORITY_HIGH_ACCURACY);
        mManager.requestLocationUpdates(IALocationRequest.create(), mListener);
        mManager.registerRegionListener(mRegionListener);
    }

    //clean memory stop request
    public void onDestroy() {
        super.onDestroy();
        mManager.destroy();
    }


    private void fetchFloorPlanBitmap(final IAFloorPlan floorPlan) {
        final String url = floorPlan.getUrl();
        if (mLoadTarget == null) {
            mLoadTarget = new Target() {

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    setupGroundOverlay(floorPlan, bitmap);
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }

            };
        }

        RequestCreator request = Picasso.with(this).load(url);

        final int bitmapWidth = floorPlan.getBitmapWidth();
        final int bitmapHeight = floorPlan.getBitmapHeight();

        if (bitmapHeight > MAX_DIMENSION) {
            request.resize(0, MAX_DIMENSION);
        } else if (bitmapWidth > MAX_DIMENSION) {
            request.resize(MAX_DIMENSION, 0);
        }

        request.into(mLoadTarget);
    }

    private IARegion.Listener mRegionListener = new IARegion.Listener() {
        @Override
        public void onEnterRegion(IARegion iaRegion) {
            if (iaRegion.getType() == IARegion.TYPE_FLOOR_PLAN) {
                if (mGroundOverlay == null || !iaRegion.equals(mOverlayFloorPlan)) {
                    update = true;
                    if (mGroundOverlay != null) {
                        mGroundOverlay.remove();
                        mGroundOverlay = null;
                    }
                    mOverlayFloorPlan = iaRegion;
                    fetchFloorPlanBitmap(iaRegion.getFloorPlan());
                } else {
                    mGroundOverlay.setTransparency(0.0f);
                }
            }

        }

        @Override
        public void onExitRegion(IARegion iaRegion) {

        }
    };

    public void btnFloor(View view) {
        String text = "ชั้น" + floor;
        Toast.makeText(getApplicationContext(),text, Toast.LENGTH_SHORT).show();
    }

}