import csv
import statistics

def get_result(fileName):
  with open(fileName) as f:
    next(f)  # Skip the header
    reader = csv.reader(f, skipinitialspace=True)
    result = dict(reader)

    # There must be one 'measured build __' for each iteration.
    # This needs to match the number of iterations being passed in PRBuildBenchmark.yml
    valuesstr = [result['measured build #1'],
              result['measured build #2'],
              result['measured build #3'],
              result['measured build #4'],
              result['measured build #5'],
              result['measured build #6'],
              result['measured build #7'],
              result['measured build #8'],
              result['measured build #9'],
              result['measured build #10']]
    values = list(map(int, valuesstr))
    return statistics.mean(values)

baseResult = get_result('profile-out/benchmark.csv')
baseMean = baseResult

buildStr = "Branch Head Build Time: " + str(headMean) + " | Base Branch Build Time: " + str(baseMean)

# print result on console and write in a file
print(buildStr)
with open("benchmark-result.txt", 'w') as f:
  f.write(buildStr)

# fields=['first','second','third']
# with open(r'name', 'a') as f:
#   writer = csv.writer(f)
#   writer.writerow(fields)
