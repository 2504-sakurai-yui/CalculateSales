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

	//商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_SERIAL_NUMBER = "売上ファイルが連番ではありません";
	private static final String SALES_AMOUNT_DIGIT_OVER = "合計金額が10桁を超えました";
	private static final String BRANCH_FILE_INVALID_KEY = "の支店コードが不正です";
	private static final String COMMODITY_FILE_INVALID_KEY = "の商品コードが不正です";
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
		//商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		//商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		//エラー処理3-1 コマンドライン引数が指定されているか
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales, "[0-9]{3}", "支店")) {
			return;
		}

		//商品定義ファイルの読み込み処理
		if (!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales, "[0-9a-zA-Z]{8}", "商品")) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		// 処理内容2-1(売上ファイルのみ取り出す)
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();

		//課題のすべてのファイルから売上ファイルの仕様に合ったものだけをrcdFilesに追加する
		for (int i = 0; i < files.length; i++) {
			//エラー処理3 売上ファイルがファイルなのか（ディレクトリではないか）
			if (!(files[i].isFile()) && files[i].getName().matches("^[0-9]{8}[.]rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		//エラー処理2-1 rcdFilesを昇順にソート
		Collections.sort(rcdFiles);
		//エラー処理2-1 売上ファイルが連番かどうか
		for (int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt((rcdFiles.get(i)).getName().substring(0, 8));
			int latter = Integer.parseInt((rcdFiles.get(i + 1)).getName().substring(0, 8));

			if (latter - former != 1) {
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
				while ((line = br.readLine()) != null) {
					saleFiles.add(line);
				}

				//エラー処理2-4 売上ファイルの中身が不正なフォーマットの場合
				if (saleFiles.size() != 3) {
					System.out.println("<" + (rcdFiles.get(i)).getName() + SALE_FILE_INVALID_FORMAT);
					return;
				}

				//エラー処理2-3 売上ファイルの支店コードが支店定義ファイルに存在しているか
				if (!branchNames.containsKey(saleFiles.get(0))) {
					System.out.println((rcdFiles.get(i)).getName() + BRANCH_FILE_INVALID_KEY);
					return;
				}
				//エラー処理 売上ファイルの商品コードが商品定義ファイルに存在しているか
				if (!commoditySales.containsKey(saleFiles.get(1))) {
					System.out.println((rcdFiles.get(i)).getName() + COMMODITY_FILE_INVALID_KEY);
				}

				//エラー処理3 売上金額が数字なのか
				if (!saleFiles.get(2).matches("^\\d{1,10}$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				//売上金額をlong型にする
				long fileSale = Long.parseLong(saleFiles.get(2));

				//売上金額を合計する
				Long branchSaleAmount = branchSales.get(saleFiles.get(0)) + fileSale;
				Long commoditySaleAmount = commoditySales.get(saleFiles.get(1)) + fileSale;

				//エラー処理2-2 合計売上金額が10桁以下か
				if ((branchSaleAmount >= 10000000000L) && (commoditySaleAmount >= 10000000000L)) {
					System.out.println(SALES_AMOUNT_DIGIT_OVER);
					return;
				}

				//branchSalesに支店コードと合計売上金額を入れる
				branchSales.put(saleFiles.get(0), branchSaleAmount);
				//commoditySalesに商品コードと合計売上金額を入れる
				commoditySales.put(saleFiles.get(1), commoditySaleAmount);

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

		// 商品別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 支店定義・商品定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コード(商品コード)と支店名(商品名)を保持するMap
	 * @param 支店コード(商品コード)と売上金額を保持するMap
	 * @param 正規表現
	 * @param フォーマットが不正の場合
	 * @param ファイルの存在チェック
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> names,
			Map<String, Long> sales, String regularExpression, String branchOrCommodity) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			//エラー処理1-1 ファイルの存在チェック
			if (!file.exists()) {
				System.out.println(branchOrCommodity + FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");

				//エラー処理1-2 支店定義ファイル(商品定義ファイル)のフォーマットが不正の場合
				if ((items.length != 2) || (!items[0].matches(regularExpression))) {
					System.out.println(branchOrCommodity + FILE_INVALID_FORMAT);
					return false;
				}

				//Namesに支店コード(商品コード)と支店名(商品名)、Salesに支店コード(商品コード)と初期値0を入れる
				names.put(items[0], items[1]);
				sales.put(items[0], 0L);

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
	private static boolean writeFile(String path, String fileName, Map<String, String> names,
			Map<String, Long> sales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			//mapからすべてのkeyを取得する→書き込む
			for (String key : names.keySet()) {
				bw.write(key + ", " + names.get(key) + ", " + sales.get(key));
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