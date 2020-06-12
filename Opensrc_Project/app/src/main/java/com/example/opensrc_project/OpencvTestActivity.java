package com.example.opensrc_project;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.imgproc.Imgproc.contourArea;

class OpencvTestActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

        JavaCameraView cameraBridgeViewBase;
        BaseLoaderCallback baseLoaderCallback;
        Point m = new Point(-1, -1);

        private static final String TAG = "OpencvTesting";
        private static final String TAG2 = "MyTesting";

        //  Vector<Vec4i> hierarchy;
        Mat mat1, mat2, mat3, m_final,m_test;
        Mat mask,zeross;

        byte[] binaryCode = new byte[36];
        byte[] binaryNew = new byte[36];

        @Override
        protected void onCreate(Bundle saveInstanceState) {
                super.onCreate(saveInstanceState);
                setContentView(R.layout.activity_opencvtesting);
                cameraBridgeViewBase = findViewById(R.id.myCameraView);
                cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
                cameraBridgeViewBase.setCvCameraViewListener(this);
                baseLoaderCallback = new BaseLoaderCallback(this) {
                        @Override
                        public void onManagerConnected(int status) {
                                super.onManagerConnected(status);
                                switch (status) {
                                        case BaseLoaderCallback.SUCCESS:
                                                cameraBridgeViewBase.enableView();
                                                break;
                                        default:
                                                super.onManagerConnected(status);
                                                break;
                                }
                        }
                };
        }
        @Override
        public void onCameraViewStarted(int width, int height) {
                mat1 = new Mat(width, height, CvType.CV_8UC4);
                mat2 = new Mat(width, height, CvType.CV_8UC4);
                mat3 = new Mat(width, height, CvType.CV_8UC4);
                m_final = new Mat(width, height, CvType.CV_8UC4);
                mask = new Mat();
                zeross = new Mat(6, 6, CvType.CV_8UC1);
        }
        @Override
        public void onCameraViewStopped() {
                mat1.release();
                mat2.release();
                mat3.release();
        }
        public List<MatOfPoint> findMarkerContours(Mat frame) {
                double minPerimeterRate = 0.9;//0.03
                double maxPerimeterRate = 4.0;
                int minPerimeterPixels =
                        (int) minPerimeterRate * Math.max(frame.cols(), frame.rows());
                int maxPerimeterPixels =
                        (int) maxPerimeterRate * Math.max(frame.cols(), frame.rows());
                mat1 = frame;
                Imgproc.cvtColor(mat1, mat3, Imgproc.COLOR_BGRA2GRAY); //convert to Greyscale Img
                Imgproc.GaussianBlur(mat3, mat2, new Size(7, 7), 3,3); //apply gaussian
                Size kss = new Size(3,3);
                mask = Imgproc.getStructuringElement((Imgproc.MORPH_CROSS),kss);
                Imgproc.dilate(mat2,mat3,mask,m,3);
                Imgproc.erode(mat3,mat1,mask,m,3);
                //Imgproc.adaptiveThreshold(mat1,mat3,260,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY,5,2);
                Imgproc.threshold(mat1,mat3,127,255,Imgproc.THRESH_BINARY);
                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                List<MatOfPoint> contours_final = new ArrayList<MatOfPoint>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(mat3, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);//1 에서 찾아서 contours array에 저장.
//contours!!!
                //filter contours
                for (int i = 0; i < contours.size(); i++) {
                        MatOfPoint contour = contours.get(i);
                        Size contour_size = contours.get(i).size();
                        if(contourArea(contour)<800)continue;
                        //check perimeter
                        if (Math.min(contour_size.width, contour_size.height) < minPerimeterPixels || Math.max(contour_size.width, contour_size.height) > maxPerimeterPixels)
                                continue;
//            //check if square and convex
                        double tempEpsilon = 0.02* Imgproc.arcLength(new MatOfPoint2f(contour.toArray()),true);
                        MatOfPoint2f approxCurve = new MatOfPoint2f();
                        Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()),approxCurve,tempEpsilon,true);
                        List<Point> approxCurveList = approxCurve.toList();
                        //근사화 했을 때, 5,3각형 제외., 뚫려있어도 제외
                        if (approxCurveList.size() != 4) {
                                continue;
                        }
//            // check min distance between corners
                        double minCornerDistanceRate = 0.05;
                        double minDistSq =Math.pow(Math.max(contour.cols(), contour.rows()), 2);
                        for(int j = 0; j < 4; j++) {
                                double d = Math.pow( (approxCurveList.get(j).x - approxCurveList.get((j + 1) % 4).x), 2 )
                                        +  Math.pow( (approxCurveList.get(j).y - approxCurveList.get((j + 1) % 4).y), 2 );
                                minDistSq = Math.min(minDistSq, d);
                        }
                        double minCornerDistancePixels = contours.get(i).total() * minCornerDistanceRate;
                        if (minDistSq < minCornerDistancePixels * minCornerDistancePixels) continue;
//
//            // check if it is too near to the image border
//  외곽근처를 빼줌.
                        int minDistanceToBorder = 3;
                        Boolean tooNearBorder = false;
                        for(int j = 0; j < 4; j++) {
                                if (approxCurveList.get(j).x < minDistanceToBorder || approxCurveList.get(j).y < minDistanceToBorder ||
                                        approxCurveList.get(j).x > frame.cols() - 1 - minDistanceToBorder ||
                                        approxCurveList.get(j).y > frame.rows() - 1 - minDistanceToBorder)
                                        tooNearBorder = true;
                        }
                        if(tooNearBorder) continue;
//            // if it passes all the test, add to candidates vector
                        MatOfPoint mPoints = new MatOfPoint();
                        mPoints.fromList(approxCurveList);
                        //인식점 사이의 거리. 쳐내
                        contours_final.add(mPoints);
                }
                return contours_final;
        }
        public Point GetVector( List<Point> a, int x, int y){ //x-y
                double tempx = a.get(x).x - a.get(y).x;
                double tempy = a.get(x).y - a.get(y).y;
                Point newpoint= new Point(0,0);
                newpoint.x = tempx;
                newpoint.y = tempy;
                return newpoint;
        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Scalar border_color = new Scalar(255, 0, 0);

                m_final = inputFrame.rgba();
                List<MatOfPoint> contours = findMarkerContours(m_final);
                //m_test = inputFrame.rgba();
                for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
                        Imgproc.drawContours(m_final, contours, contourIdx, border_color, 2); //thickness 음수면 내부 채움, contouridx 음수면 전체
                }
                //받아온 컨투어로 perspective -> frontview
                m_test = RefineMatrix(contours);
                if(m_test!=null) {
                        binaryNew = DetectCode(m_test);
                        //if (!binaryNew.equals(binaryCode)) {
                        binaryCode = binaryNew;
                        Log.e("binary", Arrays.toString(binaryNew));
                }

        else {
                binaryCode=null;
                binaryNew=null;
                Log.d(TAG, "notFounded");
        }
        //int answer = Decode(binaryCode); //최종 우리가 원하는 값
        return m_final;
}
        public byte[] DetectCode(Mat m){
                byte[] arr = new byte[36];
                int x,y;
                int k;
                for( y=0; y<6;y++){
                        for( x =0; x<6; x++){
                                k=y*6+x;
                                if(m.get(y,x)[0]==1)
                                        arr[k]=1;
                                else
                                        arr[k]=0;
                        }
                }
                return arr;
        }
        public int Decode(byte[] array){
                return 0;
        }
        public Mat RefineMatrix(List <MatOfPoint> p_mat_of_point) {//contour 을 받아서 새로운 정제된 matrix로 리턴.
                //approxCurveList를 받아와야함.
                Mat RealCodeMatrix = new Mat(); //return 할 녀석임
                Mat temp1 = new Mat();
                Mat temp2 = new Mat();
                Mat distMat = new Mat();
                Mat cell = new Mat();
                List<Point> p_point = new ArrayList<>();
                for (int k = 0; k < p_mat_of_point.size(); k++) {
                        //받아온 모든 contour에 대하여
                        p_point = p_mat_of_point.get(k).toList();
                        Point v1 = GetVector(p_point, 1, 0);//내 다음 점과 내 위치를 외적,
                        Point v2 = GetVector(p_point, 2, 0);//내 대각 점과 내 위치를 외적.
                        double o = (v1.x * v2.y) - (v1.y * v2.x);
                        if (o < 0.0) {//swap
                                Point temp = new Point(0, 0);
                                temp.x = p_point.get(1).x;
                                temp.y = p_point.get(1).y;
                                p_point.get(1).x = p_point.get(3).x;
                                p_point.get(1).y = p_point.get(3).y;
                                p_point.get(3).x = temp.x;
                                p_point.get(3).y = temp.y;
                        }
                        //Point to Mat or MatOfPoint2f.
                        MatOfPoint2f src = new MatOfPoint2f(
                                new Point(p_point.get(0).x,p_point.get(0).y),
                                new Point(p_point.get(1).x,p_point.get(1).y),
                                new Point(p_point.get(2).x,p_point.get(2).y),
                                new Point(p_point.get(3).x,p_point.get(3).y)
                        );
                        MatOfPoint2f dst = new MatOfPoint2f(
                                new Point(0, 0),
                                new Point(400-1 , 0),
                                new Point(400-1 , 400-1 ),
                                new Point(0, 400-1 )
                        );
                        distMat = Imgproc.getPerspectiveTransform(src, dst); // 찾은 점을 dst 점으로 핀것의 행렬 = distMat
                        temp1 = mat3.clone();
                        Imgproc.warpPerspective(mat3, temp1, distMat, new Size(400, 400));//source, dest , m, size(최대 width, height)
                        //Imgproc.adaptiveThreshold(temp1, mat2, 260, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 5, 2); //보간된다음이니까 0,1 상태가 아닐거다 그러니까 함더 ㄱ.
                        Imgproc.threshold(temp1,temp2,127,255,Imgproc.THRESH_BINARY);
                        //Mat src = new Mat(400, 400, CvType.CV_8UC4);
                        //Mat subView = new Mat(mat2, new Rect(10, 10, 45, 45));
                        //400x400은 짤라야해서 8,8 이고, 값은 총 36개가 나와서 6,6인 bitMatrix에 저장.
                        Mat bitMatrix = new Mat();
                        int kx = 8;
                        int ky = 8;
                        int cellSize = 50;
                        bitMatrix = zeross; //0으로 초기화.zeross 는 (6,6,CvType.8UC4)
                        for (int y = 0; y < 6; y++) {
                                for (int x = 0; x < 6; x++) {
                                        //  bitMatrix.get(y,x)[0] =0;
                                        bitMatrix.put(y,x,0);
                                }
                        }
                        //400x400 짤라서 읽으면서 white이다 싶은 녀석을 bitMatrix에 저장.
                        for (int y = 0; y < ky-2; y++) { //7에 대해선 안해도 댐
                                for (int x = 0; x < kx-2; x++) {
                                        int cellX = (x + 1) * cellSize+10;//(0,0) 기준 -> (60,60) ,,(5,5)기준 -> 310,310
                                        int cellY = (y + 1) * cellSize+10;
                                        cell = temp2.submat(new Rect(cellX, cellY, cellSize, cellSize));
                                        int nZ = Core.countNonZero(cell); //0이 아닌 녀석의 갯수를 반환한다.
                                        if (nZ > (cellSize * cellSize) / 2) {
                                                //.at
                                                //bitMatrix.get(y, x)[0] = 1;
                                                bitMatrix.put(y,x,1);
                                        }
                                }
                        }
                        //돌리기 하다가 맞으면 return
                        Mat[] rotation = new Mat[4];
                        rotation[0] = bitMatrix;
                        if (bitMatrix.get(0, 0)[0] == 1 && bitMatrix.get(0, 5)[0] == 1 && bitMatrix.get(5, 0)[0] == 1) {
                                RealCodeMatrix = rotation[0];
                                return RealCodeMatrix;
                        }
                        else{
                                for (int p = 1; p < 4; p++) {
                                        // rotate(rotation[p - 1], rotation[p], Core.ROTATE_90_CLOCKWISE);
                                        double l=90;
                                        rotation[p] = rotateMatCW(rotation[p-1],l);
                                        l+=90;
                                        if (rotation[p].get(0, 0)[0] == 1 && rotation[p].get(0, 5)[0] == 1 && rotation[p].get(5, 0)[0] == 1) {
                                                RealCodeMatrix = rotation[p];
                                                return RealCodeMatrix;
                                        }
                                }
                        }
                }
                //contour 다 돌았는데도 RealCodeMatrix를 못찾으면 return null
                return null;
        }
        public Mat rotateMatCW(Mat src,  double deg ){
                Mat returnMatrix = new Mat();
                if (deg == 270 ){
                        // Rotate clockwise 270 degrees
                        Core.transpose(src, returnMatrix);
                        Core.flip(returnMatrix, returnMatrix, 0);
                }
                else if (deg == 180 ){
                        // Rotate clockwise 180 degrees
                        Core.flip(src, returnMatrix, -1);
                }
                else if (deg == 90 ){
                        // Rotate clockwise 90 degrees
                        Core.transpose(src, returnMatrix);
                        Core.flip(returnMatrix, returnMatrix, 1);
                }
                return returnMatrix;
        }
        @Override
        protected void onPause() {
                super.onPause();
                if (cameraBridgeViewBase != null) {
                        cameraBridgeViewBase.disableView();
                }
        }
        @Override
        protected void onResume() {
                super.onResume();
                if (!OpenCVLoader.initDebug()) {
                        Toast.makeText(getApplicationContext(), "thereisaproblem in opencv", Toast.LENGTH_SHORT).show();
                        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, baseLoaderCallback);
                } else {
                        baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
                        baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                }
        }
        @Override
        protected void onDestroy() {
                super.onDestroy();
        }}