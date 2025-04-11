package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_SERIAL_NUMBER = "売上ファイルが連番ではありません";
	private static final String SALES_AMOUNT_DIGIT_OVER = "合計金額が10桁を超えました";
	private static final String FILE_INVALID_KEY = "の支店コードが不正です";
	private static final String SALE_FILE_INVALID_FORMAT = "のフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		//エラー処理3-1 コマンドライン引数が指定されているか
		if(args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		// 処理内容2-1(売上ファイルのみ取り出す)
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();

		//課題のすべてのファイルから売上ファイルの仕様に合ったものだけをrcdFilesに追加する
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().matches("^[0-9]{8}[.]rcd$")) {
				rcdFiles.add(files[i]);
			}

			//エラー処理3 売上ファイルがファイルなのか（ディレクトリではないか）
			if(!(files[i].isFile()) && !((rcdFiles.get(i)).getName()).matches("^[0-9]{8}[.]rcd$")) {
				System.out.println(UNKNOWN_ERROR);
				return;
			}
		}

		//エラー処理2-1 rcdFilesを昇順にソート
		Collections.sort(rcdFiles);
		//エラー処理2-1 売上ファイルが連番かどうか
		for(int i= 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt((rcdFiles.get(i)).getName().substring(0, 8));
			int latter = Integer.parseInt((rcdFiles.get(i + 1)).getName().substring(0, 8));

			if(latter - former != 1) {
				System.out.println(FILE_NOT_SERIAL_NUMBER);
				return;
			}
		}

		// 処理内容2-2(売上ファイル内読込、売上額を支店の合計金額に加算)
		for (int i = 0; i < rcdFiles.size(); i++) {
			BufferedReader br = null;

			try {
				File file = rcdFiles.get(i);
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				//売上ファイルの中身を一行ずつ読む→lineに格納
				List<String> saleFiles = new ArrayList<>();
				String line;
				while((line = br.readLine()) != null) {
					saleFiles.add(line);
				}

				//エラー処理2-4 売上ファイルの中身が不正なフォーマットの場合
				if(saleFiles.size() != 2) {
					System.out.println("<" + (rcdFiles.get(i)).getName() + ">" + SALE_FILE_INVALID_FORMAT);
					return;
				}

				//エラー処理2-3 売上ファイルの支店コードが支店定義ファイルに存在しているか
				if(!branchNames.containsKey(saleFiles.get(0))) {
					System.out.println("<" + (rcdFiles.get(i)).getName() + ">" + FILE_INVALID_KEY);
					return;
				}
				//エラー処理3 売上金額が数字なのか
				if(!saleFiles.get(1).matches("^\\d{1,10}$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				//売上金額をlong型にする
				long fileSale = Long.parseLong(saleFiles.get(1));

				Long saleAmount = branchSales.get(saleFiles.get(0)) + fileSale;
				//エラー処理2-2 合計売上金額が10桁以下か
				if(saleAmount >= 10000000000L) {
					System.out.println(SALES_AMOUNT_DIGIT_OVER);
					return;
				}

				//branchSalesに支店コードと合計売上金額を入れる
				branchSales.put(saleFiles.get(0), saleAmount);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;

			} finally {
				// ファイルを開いている場合
				if (br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}

		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			//エラー処理1-1 ファイルの存在チェック
			if(!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");
				//エラー処理1-2 支店定義ファイルのフォーマットが不正の場合
				if((items.length != 2) || (!items[0].matches("[0-9]{3}"))) {
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				//branchNamesに支店コードと支店名、branchSalesに支店コードと初期値0を入れる
				branchNames.put(items[0], items[1]);
				branchSales.put(items[0], 0L);

			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;

		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;

	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			//mapからすべてのkeyを取得する→書き込む
			for (String key : branchNames.keySet()) {
				bw.write(key + ", " + branchNames.get(key) + ", " + branchSales.get(key));
				//改行
				bw.newLine();
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;

		} finally {
			// ファイルを開いている場合
			if (bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;

	}

}