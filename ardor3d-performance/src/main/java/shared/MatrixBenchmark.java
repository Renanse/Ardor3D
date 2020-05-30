
package shared;

public interface MatrixBenchmark {

  void resetRandom(long seed);

  String getPlatformName();

  ResultSample doTransformMultTest(int count, int maxCount, long timeOutMS);

  ResultSample doTransformPointTest(int count, int maxCount, long timeOutMS);

  ResultSample doTransformVectorTest(int count, int maxCount, long timeOutMS);

  ResultSample doInverse3Test(int count, int maxCount, long timeOutMS);

  ResultSample doInverseTest(int count, int maxCount, long timeOutMS);

  ResultSample doTransposeTest(int count, int maxCount, long timeOutMS);

  ResultSample doRotateTest2(int count, int maxCount, long timeOutMS);

  ResultSample doRotateTest1(int count, int maxCount, long timeOutMS);

  ResultSample doScaleTest(int count, int maxCount, long timeOutMS);

  ResultSample doTranslateTest(int count, int maxCount, long timeOutMS);

  ResultSample doMultTest(int count, int maxCount, long timeOutMS);

}
