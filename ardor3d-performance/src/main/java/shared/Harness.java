
package shared;

import java.util.Arrays;

import com.ardor3d.performancetest.Ardor3DMatrixBenchmark;
import com.vecmath.performancetest.VecmathMatrixBenchmark;

public class Harness {

    public static void main(final String[] args) {
        final MatrixBenchmark[] benchmarks = new MatrixBenchmark[2];
        benchmarks[0] = new Ardor3DMatrixBenchmark();
        benchmarks[1] = new VecmathMatrixBenchmark();

        System.out.format("Warming up.");
        Harness.runTests(benchmarks, 1, false);
        System.out.format(".");
        Harness.runTests(benchmarks, 1, false);
        System.out.format(".%n");
        Harness.runTests(benchmarks, 1, false);
        System.out.format("Running tests...%n");
        Harness.runTests(benchmarks, 100, true);
    }

    private static void runTests(final MatrixBenchmark[] benchmarks, final int loops, final boolean print) {
        Harness.printResults("Transform Multiply", Harness.runTests(benchmarks, loops, "TransformMult"), benchmarks,
                print);
        Harness.printResults("4x4 Multiplication - Ma*Mb", Harness.runTests(benchmarks, loops, "Mult"), benchmarks,
                print);
        Harness.printResults("Inverse 4x4", Harness.runTests(benchmarks, loops, "Inverse"), benchmarks, print);
        Harness.printResults("Inverse 3x3", Harness.runTests(benchmarks, loops, "Inverse3"), benchmarks, print);
        Harness.printResults("Rotation (Arbitrary axis) - M*Mr", Harness.runTests(benchmarks, loops, "Rotate1"),
                benchmarks, print);
        Harness.printResults("Rotation (X axis) - M*Mrx", Harness.runTests(benchmarks, loops, "Rotate2"), benchmarks,
                print);
        Harness.printResults("Scaling of Matrix4", Harness.runTests(benchmarks, loops, "Scale"), benchmarks, print);
        Harness.printResults("Point Transformation (M*p)", Harness.runTests(benchmarks, loops, "TransformPoint"),
                benchmarks, print);
        Harness.printResults("Vector Transformation (M*v)", Harness.runTests(benchmarks, loops, "TransformVector"),
                benchmarks, print);
        Harness.printResults("Translation of Matrix4", Harness.runTests(benchmarks, loops, "Translate"), benchmarks,
                print);
        Harness.printResults("Transpose of Matrix4", Harness.runTests(benchmarks, loops, "Transpose"), benchmarks,
                print);
    }

    private static void printResults(final String label, final TestResults[] results,
            final MatrixBenchmark[] benchmarks, final boolean doPrint) {
        if (!doPrint) {
            return;
        }
        System.out.format("%nTest: %s%n", label);
        for (int i = 0; i < results.length; i++) {
            if (results[i] != null) {
                System.out.format("\t" + benchmarks[i].getPlatformName()
                        + ": Avg: %,.0f ips, Min: %,.0f ips, Max: %,.0f ips%n", results[i].avg, results[i].min,
                        results[i].max);
            } else {
                System.out.format("\t" + benchmarks[i].getPlatformName() + ": n/a%n");
            }
        }
    }

    private static TestResults[] runTests(final MatrixBenchmark[] benchmarks, final int innerLoops,
            final String testName) {
        final TestResults[] results = new TestResults[benchmarks.length];
        int index = 0;
        ResultSample baseResult = null;
        for (final MatrixBenchmark benchmark : benchmarks) {
            // run a sample to collect data
            benchmark.resetRandom(0);
            final ResultSample pretest = Harness.test(benchmark, testName, 5, 1, 1000);
            if (pretest == null) {
                index++;
                continue;
            }
            if (baseResult == null && pretest != null) {
                baseResult = pretest;
            } else if (pretest != null) {
                if (!Harness.compare(pretest, baseResult)) {
                    System.out.format("%n\t\t%s doesn't match baseline results.%n", benchmark.getPlatformName());
                    System.out.format("baseline: %s%n", Arrays.toString(baseResult.result));
                    System.out.format("versus: %s", Arrays.toString(pretest.result));
                }
            }

            benchmark.resetRandom(0);
            int totalIterations = 0;
            int minIterations = 0;
            int maxIterations = 0;
            long totalTime = 0;
            long minTime = 0;
            long maxTime = 0;
            for (int i = 0; i < 10; i++) {
                final ResultSample sample = Harness.test(benchmark, testName, innerLoops, -1, 30);
                final int iterations = sample.loopCount;
                final long time = sample.time;
                if (i == 0) {
                    minIterations = iterations;
                    maxIterations = iterations;
                    minTime = time;
                    maxTime = time;
                } else {
                    if (minIterations > iterations) {
                        minIterations = iterations;
                        minTime = time;
                    }
                    if (maxIterations < iterations) {
                        maxIterations = iterations;
                        maxTime = time;
                    }
                }
                totalIterations += iterations;
                totalTime += time;
            }

            final double iterationsPerSecond = Math.floor(totalIterations / (totalTime * 0.001));
            final double minIterationsPerSecond = Math.floor(minIterations / (minTime * 0.001));
            final double maxIterationsPerSecond = Math.floor(maxIterations / (maxTime * 0.001));

            final TestResults result = new TestResults();
            results[index++] = result;
            result.avg = iterationsPerSecond;
            result.min = minIterationsPerSecond;
            result.max = maxIterationsPerSecond;
            result.result = pretest.result;
        }
        return results;
    }

    private static boolean compare(final ResultSample sampleA, final ResultSample sampleB) {
        if (sampleA.result.length != sampleB.result.length) {
            return false;
        }

        for (int i = 0; i < sampleA.result.length; i++) {
            if (Math.abs(sampleA.result[i] - sampleB.result[i]) > 0.001) {
                return false;
            }
        }
        return true;
    }

    private static ResultSample test(final MatrixBenchmark benchmark, final String testName, final int count,
            final int maxCount, final long timeOutMS) {
        if ("TransformMult".equalsIgnoreCase(testName)) {
            return benchmark.doTransformMultTest(count, maxCount, timeOutMS);
        } else if ("Mult".equalsIgnoreCase(testName)) {
            return benchmark.doMultTest(count, maxCount, timeOutMS);
        } else if ("TransformPoint".equalsIgnoreCase(testName)) {
            return benchmark.doTransformPointTest(count, maxCount, timeOutMS);
        } else if ("TransformVector".equalsIgnoreCase(testName)) {
            return benchmark.doTransformVectorTest(count, maxCount, timeOutMS);
        } else if ("Inverse3".equalsIgnoreCase(testName)) {
            return benchmark.doInverse3Test(count, maxCount, timeOutMS);
        } else if ("Inverse".equalsIgnoreCase(testName)) {
            return benchmark.doInverseTest(count, maxCount, timeOutMS);
        } else if ("Rotate1".equalsIgnoreCase(testName)) {
            return benchmark.doRotateTest1(count, maxCount, timeOutMS);
        } else if ("Rotate2".equalsIgnoreCase(testName)) {
            return benchmark.doRotateTest2(count, maxCount, timeOutMS);
        } else if ("Scale".equalsIgnoreCase(testName)) {
            return benchmark.doScaleTest(count, maxCount, timeOutMS);
        } else if ("Translate".equalsIgnoreCase(testName)) {
            return benchmark.doTranslateTest(count, maxCount, timeOutMS);
        } else if ("Transpose".equalsIgnoreCase(testName)) {
            return benchmark.doTransposeTest(count, maxCount, timeOutMS);
        }
        return null;
    }
}
