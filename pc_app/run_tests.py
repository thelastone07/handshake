import pytest 

def run_all_tests():

    results = pytest.main(["-v", "crypto/"])
    if results == 0:
        print("All tests passed!")
    else:
        print("Some tests failed.")

if __name__ == "__main__":
    run_all_tests()