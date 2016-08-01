package jed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * JED class JED_Get_Cartesian_PCA: Gets the PCA using COV and CORR for the Cartesian subset.
 * Copyright (C) 2012 Dr. Charles David
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/license>
 * 
 * @author Dr. Charles David
 */
public class JED_Get_Cartesian_PCA
{

	public String directory, out_dir_cPCA, out_dir_COV, out_dir_CORR, description, file_name_head, path;
	public int number_of_modes, number_of_residues, ROWS, COLS;
	public double trace_COV, trace_CORR, cond_COV, cond_CORR;
	public List<Double> eigenvalues_COV, top_eigenvalues_COV, eigenvalues_CORR, top_eigenvalues_CORR;
	public double[] pca_mode_COV_min, pca_mode_COV_max, pca_mode_CORR_min, pca_mode_CORR_max;
	static Matrix input_data, centered_input_data, cov, corr, top_evectors_COV, square_pca_modes_COV, weighted_square_pca_modes_COV, weighted_pca_modes_COV, top_evectors_CORR, square_pca_modes_CORR,
			weighted_square_pca_modes_CORR, weighted_pca_modes_CORR, pca_modes_COV, pca_modes_CORR;
	public EigenvalueDecomposition evd;
	public NumberFormat nf;
	public RoundingMode rm;
	public PCA pca;
	public boolean success, exist;

	/**
	 * Constructor to perform the Cartesian PCA
	 * 
	 * @param data
	 *            The Cartesian subset coordinates
	 * @param num_modes
	 *            The number of PCA modes to process
	 * @param dir
	 *            The working directory
	 * @param des
	 *            The job description
	 */
	JED_Get_Cartesian_PCA(Matrix data, int num_modes, String dir, String des)
		{

			nf = NumberFormat.getInstance();
			rm = RoundingMode.HALF_UP;
			nf.setRoundingMode(rm);
			nf.setMaximumFractionDigits(3);
			nf.setMinimumFractionDigits(3);

			directory = dir;
			description = des;

			out_dir_cPCA = directory + "JED_RESULTS_" + description + "/cPCA/";
			exist = new File(out_dir_cPCA).exists();
			if (!exist) success = (new File(out_dir_cPCA)).mkdirs();

			out_dir_COV = out_dir_cPCA + "COV/";
			exist = new File(out_dir_COV).exists();
			if (!exist) success = (new File(out_dir_COV)).mkdirs();

			out_dir_CORR = out_dir_cPCA + "CORR/";
			exist = new File(out_dir_CORR).exists();
			if (!exist) success = (new File(out_dir_CORR)).mkdirs();

			number_of_modes = num_modes;
			input_data = data;
			data = null;
			ROWS = input_data.getRowDimension();
			COLS = number_of_modes;
			number_of_residues = (ROWS / 3);
		}

	/* ***************************** PRIMARY METHODS ************************************************ */

	/**
	 * Performs the COV and CORR PCA methods
	 */
	public void get_Cartesian_PCA()
		{

			pca = new PCA(input_data);

			input_data = null;
			System.gc();

			Do_Cov_PCA();
			Do_Corr_PCA();

		}

	private void Do_Cov_PCA()
		{
			cov = pca.get_covariance_matrix_elegant();

			Matrix centroids = pca.getData_means();
			Matrix sigma = pca.getData_sigmas();

			file_name_head = out_dir_cPCA + "ss_" + number_of_residues;
			path = file_name_head + "_centroids_of_variables.txt";
			Matrix_IO.write_Matrix(centroids, path, 12, 6);
			path = file_name_head + "_std_devs_of_centered_variables.txt";
			Matrix_IO.write_Matrix(sigma, path, 12, 6);

			System.gc();

			file_name_head = out_dir_COV + "ss_" + number_of_residues;
			Matrix_IO.write_Matrix(cov, file_name_head + "_COV_matrix.txt", 12, 6);

			evd = pca.get_eigenvalue_decomposition(cov);
			get_eigenvalues_COV();
			write_top_evals_COV();
			get_top_evects_and_reverse_COV();
			construct_PCA_Modes_COV();

			evd = null;
			System.gc();
		}

	private void Do_Corr_PCA()
		{

			corr = pca.get_R_from_Q(cov);

			cov = null;
			System.gc();

			file_name_head = out_dir_CORR + "ss_" + number_of_residues;
			Matrix_IO.write_Matrix(corr, file_name_head + "_CORR_matrix.txt", 12, 6);

			evd = pca.get_eigenvalue_decomposition(corr);

			corr = null;
			System.gc();

			get_eigenvalues_CORR();
			write_top_evals_CORR();
			get_top_evects_and_reverse_CORR();
			construct_PCA_Modes_CORR();

			evd = null;
			System.gc();
		}

	/* ********************************** COV METHODS ************************************************* */

	private void get_eigenvalues_COV()
		{

			double[] ss_evals = evd.getRealEigenvalues();
			trace_COV = 0;
			eigenvalues_COV = new ArrayList<Double>();
			for (double k : ss_evals)
				{
					eigenvalues_COV.add(k);
					trace_COV += k;
				}
			Collections.sort(eigenvalues_COV, Collections.reverseOrder());
			double min = eigenvalues_COV.get(ROWS - 7); // first 6 eigenvalues are 'zero' due to removal of 3 translational and 3 rotational degrees of freedom.
			double max = eigenvalues_COV.get(0);
			cond_COV = Math.abs(max / min);
			file_name_head = out_dir_COV + "ss_" + number_of_residues;
			List_IO.write_Double_List(eigenvalues_COV, file_name_head + "_eigenvalues_COV.txt", 12);
		}

	private void write_top_evals_COV()
		{
			try
				{
					file_name_head = out_dir_COV + "ss_" + number_of_residues;
					File top_ss_evals_cov = new File(file_name_head + "_top_" + number_of_modes + "_eigenvalues_COV.txt");
					BufferedWriter top_ss_evals_writer = new BufferedWriter(new FileWriter(top_ss_evals_cov));
					top_ss_evals_writer.write(String.format("%-16s%-16s%-16s%n", "Eigenvalue", "% Variance", "Cumulative Variance"));
					top_eigenvalues_COV = new ArrayList<Double>();
					double cumulative_variance = 0;
					for (int e = 0; e < number_of_modes; e++)
						{
							double val = eigenvalues_COV.get(e);
							double normed_val = (val / trace_COV);
							cumulative_variance += normed_val;
							top_ss_evals_writer.write(String.format("%-16s%-16s%-16s%n", nf.format(val), nf.format(normed_val), nf.format(cumulative_variance)));
							top_eigenvalues_COV.add(val);
						}
					top_ss_evals_writer.close();
				} catch (IOException io)
				{
					System.err.println("Could not write to the file: " + file_name_head + "_top_" + number_of_modes + "_eigenvalues_COV.txt");
					io.getMessage();
					io.getStackTrace();
				}
		}

	private void get_top_evects_and_reverse_COV()
		{

			Matrix ss_evectors = evd.getV();
			Matrix D = evd.getD();
			Matrix precision = ss_evectors.times(D.inverse()).times(ss_evectors.transpose());
			evd = null;
			System.gc();

			top_evectors_COV = ss_evectors.getMatrix(0, ROWS - 1, ROWS - number_of_modes, ROWS - 1);
			Matrix modes_reversed = new Matrix(ROWS, COLS);
			for (int r = 0; r < COLS; r++)
				{
					Matrix col = top_evectors_COV.getMatrix(0, ROWS - 1, COLS - 1 - r, COLS - 1 - r);
					modes_reversed.setMatrix(0, ROWS - 1, r, r, col);
				}
			top_evectors_COV = modes_reversed;

			file_name_head = out_dir_COV + "ss_" + number_of_residues;
			path = file_name_head + "_top_" + number_of_modes + "_eigenvectors_COV.txt";
			Matrix_IO.write_Matrix(top_evectors_COV, path, 12, 6);
			path = file_name_head + "_PRECISION_matrix.txt";
			Matrix_IO.write_Matrix(precision, path, 12, 3);
			ss_evectors = null;
			System.gc();
		}

	private void construct_PCA_Modes_COV()
		{

			pca_modes_COV = new Matrix(number_of_residues, number_of_modes);
			weighted_pca_modes_COV = new Matrix(number_of_residues, number_of_modes);
			square_pca_modes_COV = new Matrix(number_of_residues, number_of_modes);
			weighted_square_pca_modes_COV = new Matrix(number_of_residues, number_of_modes);
			pca_mode_COV_max = new double[number_of_modes];
			pca_mode_COV_min = new double[number_of_modes];
			for (int a = 0; a < number_of_modes; a++)
				{
					double max = 0;
					double min = 1;
					for (int b = 0; b < number_of_residues; b++)
						{
							double x = top_evectors_COV.get(b, a);
							double y = top_evectors_COV.get(b + number_of_residues, a);
							double z = top_evectors_COV.get((b + 2 * number_of_residues), a);
							double sq = (Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
							double sqrt_sq = Math.sqrt(sq);
							double value = eigenvalues_COV.get(a);
							double sqrt_val = Math.sqrt(value);
							double w_sq = sq * value;
							double w_m = sqrt_sq * sqrt_val;
							pca_modes_COV.set(b, a, sqrt_sq);
							weighted_pca_modes_COV.set(b, a, w_m);
							square_pca_modes_COV.set(b, a, sq);
							weighted_square_pca_modes_COV.set(b, a, w_sq);
							if (sq > max) max = sq;
							if (sq < min) min = sq;
						}
					pca_mode_COV_max[a] = max;
					pca_mode_COV_min[a] = min;
				}
			file_name_head = out_dir_COV + "ss_" + number_of_residues;
			path = file_name_head + "_top_" + number_of_modes + "_square_pca_mode_MAXES_COV.txt";
			Array_IO.write_Double_Array(pca_mode_COV_max, path, 6);
			path = file_name_head + "_top_" + number_of_modes + "_square_pca_mode_MINS_COV.txt";
			Array_IO.write_Double_Array(pca_mode_COV_min, path, 6);
			path = file_name_head + "_top_" + number_of_modes + "_pca_modes_COV.txt";
			Matrix_IO.write_Matrix(pca_modes_COV, path, 12, 6);
			path = file_name_head + "_top_" + number_of_modes + "_weighted_pca_modes_COV.txt";
			Matrix_IO.write_Matrix(weighted_pca_modes_COV, path, 12, 6);
			path = file_name_head + "_top_" + number_of_modes + "_square_pca_modes_COV.txt";
			Matrix_IO.write_Matrix(square_pca_modes_COV, path, 12, 6);
			path = file_name_head + "_top_" + number_of_modes + "_weighted_square_pca_modes_COV.txt";
			Matrix_IO.write_Matrix(weighted_square_pca_modes_COV, path, 12, 6);

		}

	/* *********************************** CORR METHODS ************************************************** */

	private void get_eigenvalues_CORR()
		{

			double[] ss_evals = evd.getRealEigenvalues();
			trace_CORR = 0;
			eigenvalues_CORR = new ArrayList<Double>();
			for (double k : ss_evals)
				{
					eigenvalues_CORR.add(k);
					trace_CORR += k;
				}
			Collections.sort(eigenvalues_CORR, Collections.reverseOrder());
			double min = eigenvalues_CORR.get(ROWS - 7); // first 6 eigenvalues are 'zero' due to removal of 3 translational and 3 rotational degrees of
															// freedom.
			double max = eigenvalues_CORR.get(0);
			cond_CORR = Math.abs(max / min);
			file_name_head = out_dir_CORR + "ss_" + number_of_residues;
			List_IO.write_Double_List(eigenvalues_CORR, file_name_head + "_eigenvalues_CORR.txt", 12);
		}

	private void write_top_evals_CORR()
		{
			try
				{
					file_name_head = out_dir_CORR + "ss_" + number_of_residues;
					File top_ss_evals_cov = new File(file_name_head + "_top_" + number_of_modes + "_eigenvalues_CORR.txt");
					BufferedWriter top_ss_evals_writer = new BufferedWriter(new FileWriter(top_ss_evals_cov));
					top_ss_evals_writer.write(String.format("%-16s%-16s%-16s%n", "Eigenvalue", "% Variance", "Cumulative Variance"));
					top_eigenvalues_CORR = new ArrayList<Double>();
					double cumulative_variance = 0;
					for (int i = 0; i < number_of_modes; i++)
						{
							double val = eigenvalues_CORR.get(i);
							double normed_val = (val / trace_CORR);
							cumulative_variance += normed_val;
							top_ss_evals_writer.write(String.format("%-16s%-16s%-16s%n", nf.format(val), nf.format(normed_val), nf.format(cumulative_variance)));
							top_eigenvalues_CORR.add(val);
						}
					top_ss_evals_writer.close();
				} catch (IOException io)
				{
					System.err.println("Could not write to the file: " + file_name_head + "_top_" + number_of_modes + "_eigenvalues_CORR.txt");
					io.getMessage();
					io.getStackTrace();
				}
		}

	private void get_top_evects_and_reverse_CORR()
		{

			Matrix ss_evectors = evd.getV();
			Matrix D = evd.getD();
			Matrix precision = ss_evectors.times(D.inverse()).times(ss_evectors.transpose());
			evd = null;
			System.gc();
			top_evectors_CORR = ss_evectors.getMatrix(0, ROWS - 1, ROWS - number_of_modes, ROWS - 1);
			Matrix modes_reversed = new Matrix(ROWS, COLS);
			for (int r = 0; r < COLS; r++)
				{
					Matrix col = top_evectors_CORR.getMatrix(0, ROWS - 1, COLS - 1 - r, COLS - 1 - r);
					modes_reversed.setMatrix(0, ROWS - 1, r, r, col);
				}
			top_evectors_CORR = modes_reversed;
			file_name_head = out_dir_CORR + "ss_" + number_of_residues;
			path = file_name_head + "_top_" + number_of_modes + "_eigenvectors_CORR.txt";
			Matrix_IO.write_Matrix(top_evectors_CORR, path, 12, 6);
			path = file_name_head + "_PRECISION_matrix.txt";
			Matrix_IO.write_Matrix(precision, path, 12, 3);
			ss_evectors = null;
			System.gc();
		}

	private void construct_PCA_Modes_CORR()
		{

			pca_modes_CORR = new Matrix(number_of_residues, number_of_modes);
			weighted_pca_modes_CORR = new Matrix(number_of_residues, number_of_modes);
			square_pca_modes_CORR = new Matrix(number_of_residues, number_of_modes);
			weighted_square_pca_modes_CORR = new Matrix(number_of_residues, number_of_modes);
			pca_mode_CORR_max = new double[number_of_modes];
			pca_mode_CORR_min = new double[number_of_modes];
			for (int a = 0; a < number_of_modes; a++)
				{
					double max = 0;
					double min = 1;
					for (int b = 0; b < number_of_residues; b++)
						{
							double x = top_evectors_CORR.get(b, a);
							double y = top_evectors_CORR.get(b + number_of_residues, a);
							double z = top_evectors_CORR.get((b + 2 * number_of_residues), a);
							double sq = (Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
							double sqrt_sq = Math.sqrt(sq);
							double value = eigenvalues_CORR.get(a);
							double sqrt_val = Math.sqrt(value);
							double w_sq = sq * value;
							double w_m = sqrt_sq * sqrt_val;
							pca_modes_CORR.set(b, a, sqrt_sq);
							weighted_pca_modes_CORR.set(b, a, w_m);
							square_pca_modes_CORR.set(b, a, sq);
							weighted_square_pca_modes_CORR.set(b, a, w_sq);
							if (sq > max) max = sq;
							if (sq < min) min = sq;
						}
					pca_mode_CORR_max[a] = max;
					pca_mode_CORR_min[a] = min;
				}
			file_name_head = out_dir_CORR + "ss_" + number_of_residues;
			path = file_name_head + "_top_" + number_of_modes + "_square_pca_mode_MAXES_CORR.txt";
			Array_IO.write_Double_Array(pca_mode_CORR_max, path, 6);
			path = file_name_head + "_top_" + number_of_modes + "_square_pca_mode_MINS_CORR.txt";
			Array_IO.write_Double_Array(pca_mode_CORR_min, path, 6);
			path = file_name_head + "_top_" + number_of_modes + "_pca_modes_CORR.txt";
			Matrix_IO.write_Matrix(pca_modes_CORR, path, 12, 6);
			path = file_name_head + "_top_" + number_of_modes + "_weighted_pca_modes_CORR.txt";
			Matrix_IO.write_Matrix(weighted_pca_modes_CORR, path, 12, 6);
			path = file_name_head + "_top_" + number_of_modes + "_square_pca_modes_CORR.txt";
			Matrix_IO.write_Matrix(square_pca_modes_CORR, path, 12, 6);
			path = file_name_head + "_top_" + number_of_modes + "_weighted_square_pca_modes_CORR.txt";
			Matrix_IO.write_Matrix(weighted_square_pca_modes_CORR, path, 12, 6);
		}

	/* ************************************* GETTERS *********************************************** */

	/**
	 * Returns the condition number of the covariance matrix
	 * 
	 * @return
	 */
	public double get_cond_COV()
		{

			return cond_COV;
		}

	/**
	 * Returns the condition number of the correlation matrix
	 * 
	 * @return
	 */
	public double get_cond_CORR()
		{

			return cond_CORR;
		}

	/**
	 * Returns the Trace of the covariance matrix
	 * 
	 * @return
	 */
	public double get_trace_COV()
		{

			return trace_COV;
		}

	/**
	 * Returns the Trace of the correlation matrix
	 * 
	 * @return
	 */
	public double get_trace_CORR()
		{

			return trace_CORR;
		}

	/**
	 * Returns the Eigenvalues of the covariance matrix
	 * 
	 * @return
	 */
	public List<Double> getEigenvalues_COV()
		{

			return eigenvalues_COV;
		}

	/**
	 * Returns the Eigenvalues of the correlation matrix
	 * 
	 * @return
	 */
	public List<Double> getEigenvalues_CORR()
		{

			return eigenvalues_CORR;
		}

	/**
	 * Returns the array of the PCA mode minimums from the COV analysis
	 * 
	 * @return
	 */
	public double[] get_pca_mode_COV_min()
		{

			return pca_mode_COV_min;
		}

	/**
	 * Returns the array of the PCA mode maximums from the COV analysis
	 * 
	 * @return
	 */
	public double[] get_pca_mode_COV_max()
		{

			return pca_mode_COV_max;
		}

	/**
	 * Returns the array of the PCA mode minimums from the CORR analysis
	 * 
	 * @return
	 */
	public double[] get_pca_mode_CORR_min()
		{

			return pca_mode_CORR_min;
		}

	/**
	 * Returns the array of the PCA mode maximums from the CORR analysis
	 * 
	 * @return
	 */
	public double[] get_pca_mode_CORR_max()
		{

			return pca_mode_CORR_max;
		}

	/**
	 * Returns the Top eigenvectors from the COV analysis
	 * 
	 * @return
	 */
	public Matrix getTop_evectors_COV()
		{

			return top_evectors_COV;
		}

	/**
	 * Returns the Square PCA modes from the COV analysis
	 * 
	 * @return
	 */
	public Matrix getSquare_pca_modes_COV()
		{

			return square_pca_modes_COV;
		}

	/**
	 * Returns the Weighted Square PCA modes from the COV analysis
	 * 
	 * @return
	 */
	public Matrix getWeighted_square_pca_modes_COV()
		{

			return weighted_square_pca_modes_COV;
		}

	/**
	 * Returns the Weighted PCA modes from the COV analysis
	 * 
	 * @return
	 */
	public Matrix getWeighted_pca_modes_COV()
		{

			return weighted_pca_modes_COV;
		}

	public Matrix getTop_evectors_CORR()
		{

			return top_evectors_CORR;
		}

	/**
	 * Returns the Square PCA modes from the CORR analysis
	 * 
	 * @return
	 */
	public Matrix getSquare_pca_modes_CORR()
		{

			return square_pca_modes_CORR;
		}

	/**
	 * Returns the Weighted Square PCA modes from the CORR analysis
	 * 
	 * @return
	 */
	public Matrix getWeighted_square_pca_modes_CORR()
		{

			return weighted_square_pca_modes_CORR;
		}

	/**
	 * Returns the Weighted PCA modes from the CORR analysis
	 * 
	 * @return
	 */
	public Matrix getWeighted_pca_modes_CORR()
		{

			return weighted_pca_modes_CORR;
		}

	/**
	 * Returns the PCA modes from the COV analysis
	 * 
	 * @return
	 */
	public Matrix getPca_modes_COV()
		{

			return pca_modes_COV;
		}

	/**
	 * Returns the PCA modes from the CORR analysis
	 * 
	 * @return
	 */
	public Matrix getPca_modes_CORR()
		{

			return pca_modes_CORR;
		}

	/**
	 * Returns the Top eigenvalues from the COV analysis
	 * 
	 * @return
	 */
	public List<Double> getTop_eigenvalues_COV()
		{

			return top_eigenvalues_COV;
		}

	/**
	 * Returns the Top eigenvalues from the CORR analysis
	 * 
	 * @return
	 */
	public List<Double> getTop_eigenvalues_CORR()
		{

			return top_eigenvalues_CORR;
		}
}