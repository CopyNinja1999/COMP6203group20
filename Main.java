package group20;
import javax.xml.transform.Templates;

import org.apache.commons.math3.*;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.EigenDecomposition;

public class Main {
	public static RealMatrix pseudoInverse(RealMatrix x) {
		RealMatrix tempMatrix=x.transpose().multiply(x);
		RealMatrix pMatrix=new SingularValueDecomposition(tempMatrix).getSolver().getInverse().multiply(x.transpose());
		
//		System.out.print(pMatrix);
		return pMatrix;
	}

	public static void main(String[] args) {
		// Create a real matrix with two rows and three columns, using a factory
		// method that selects the implementation class for us.
		double[][] matrixData = { {1,2,3}, {2,5,3}};
		double[][]matrix={{1,2,3}};
		double[][] tMatrix=new double[3][1];
		for(int i=0;i<3;i++)
		tMatrix[0][0]= 1;
		tMatrix[1][0]=2;
		tMatrix[1][0]=3;
		RealMatrix targetMatrix=MatrixUtils.createRealMatrix(matrix);
		RealMatrix inputMatrix=MatrixUtils.createRealMatrix(matrixData);
		RealMatrix testMatrix=MatrixUtils.createRealMatrix(tMatrix);
		System.out.println(testMatrix.toString());
		RealMatrix m = MatrixUtils.createRealMatrix(matrixData);
		System.out.print(m.toString());
		// One more with three rows, two columns, this time instantiating the
		// RealMatrix implementation class directly.
//		double[][] matrixData2 = { {1d,2d}, {2d,5d}, {1d, 7d}};
//		RealMatrix n = new Array2DRowRealMatrix(matrixData2);

		// Note: The constructor copies  the input double[][] array in both cases.

		// Now multiply m by n
//		RealMatrix p = inputMatrix.multiply(targetMatrix.transpose());
//		System.out.println(p.toString());
//		System.out.println(p.getRowDimension());    // 2
//		System.out.println(p.getColumnDimension()); // 2
		
		// Invert p, using LU decomposition
		RealMatrix pInverse = pseudoInverse(inputMatrix);
		for(int i=0;i<pInverse.getRowDimension();i++) {
			double [] row=pInverse.getRow(i);
			System.out.println(row[0]);
		}
		System.out.println(pInverse.toString());
		
	}

}
