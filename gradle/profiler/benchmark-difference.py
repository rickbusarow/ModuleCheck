import csv

def get_result(fileName):
    with open(fileName) as f:
        next(f)  # Skip the header
        reader = csv.reader(f, skipinitialspace=True)
        return dict(reader)

baseResult = get_result('profile-out/benchmark.csv')
mergeResult = get_result('profile-out-head/benchmark.csv')

baseMean = baseResult['mean']
headMean = mergeResult['mean']

buildStr = "Branch Head Build Time: " + headMean + " | Base Branch Build Time: " + baseMean
# print result on console and write in a file
print buildStr
with open("benchmark-result.txt", 'w') as f:
    f.write(buildStr)
